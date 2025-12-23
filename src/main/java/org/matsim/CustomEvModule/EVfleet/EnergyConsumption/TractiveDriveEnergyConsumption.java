package org.matsim.CustomEvModule.EVfleet.EnergyConsumption;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.CustomEvModule.EVfleet.EvFleetManager;
import org.matsim.CustomEvModule.EVfleet.EvModel;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.fleet.ElectricVehicle;

/**
 * Modello di consumo EV basato sulle forze di trazione,
 * calibrato per simulazioni MATSim.
 *
 * Include:
 * - efficienza dipendente dalla velocità
 * - stop&go esplicito (congestione)
 * - limite di rigenerazione
 * - aerodinamica ridotta a basse velocità
 * Modello di consumo basato sulla trazione (Tractive Energy Model), che simula
 * l'energia necessaria per superare le forze di resistenza: rotolamento, aerodinamica, pendenza.
 *
 * Utilizza la velocità media sul link e parametri fisici stimati dal EvModel.
 * NOTA: Richiede che l'attributo "slope" (pendenza in gradi) sia eventualmente disponibile sul Link.
 */
public class TractiveDriveEnergyConsumption implements DriveEnergyConsumption {

    private static final Logger log = LogManager.getLogger(TractiveDriveEnergyConsumption.class);

    // Costanti fisiche
    private static final double GRAVITY     = 9.81;       // Accelerazione di gravità (m/s^2)
    private static final double AIR_DENSITY = 1.225;      // Densità dell'aria (kg/m^3)

    // Efficienze
    private static final double REGEN_EFFICIENCY  = 0.75;      // Efficienza rigenerativa
    private static final double MAX_REGEN_POWER_W = 50_000; // 50 kW

    // Veicolo e fleet manager
    private final ElectricVehicle electricVehicle;
    private final EvFleetManager fleetManager;

    public TractiveDriveEnergyConsumption(ElectricVehicle electricVehicle, EvFleetManager fleetManager) {
        this.electricVehicle = electricVehicle;
        this.fleetManager = fleetManager;
    }

    /**
     * Pendenza del link:
     * supporta percentuale o angolo.
     */
    private double getSlopeAngleRadians(Link link) {
        try {
            Double slope = (Double) link.getAttributes().getAttribute("slope");
            if (slope != null) {
                if (Math.abs(slope) > 0.3) {
                    slope = slope / 100.0; // percentuale
                }
                return Math.atan(slope);
            }
        } catch (Exception e) {
            // fallback
        }
        return 0.0;
    }

    /**
     * Massa stimata del veicolo.
     */
    private double calculateMassEstimate(EvModel evData) {
        final double BASE_MASS_KG = 1300.0;
        final double BATTERY_KG_PER_KWH = 5.0;

        double mass = BASE_MASS_KG +
                evData.getNominalCapacityKwh() * BATTERY_KG_PER_KWH;

        if (evData.getSegment().equalsIgnoreCase("SUV")) {
            mass += 200.0;
        } else if (evData.getSegment().equalsIgnoreCase("Hatchback")) {
            mass -= 100.0;
        }
        return mass;
    }

    /**
     * Stima il coefficiente di resistenza al rotolamento Cr -> assumo 0.01 perchè ci metti dei pneumatici belli su un auto nuova 
     */
    private double calculateCrEstimate() {
        return 0.010;
    }

    /**
    * Stima il prodotto Cd*A (coefficiente aerodinamico * area frontale) in m^2
    * basato sul tipo di carrozzeria e dimensioni del veicolo
    */
    private double calculateCdaEstimate(EvModel evData) {
        double cd;
        String body = evData.getCarBodyType().toLowerCase();

        if (body.contains("suv") || body.contains("van") || body.contains("wagon") || body.contains("estate")) cd = 0.35;
        else if (body.contains("hatchback") || body.contains("crossover")) cd = 0.32;
        else if (body.contains("sedan") || body.contains("coupe") || body.contains("cabriolet") || body.contains("liftback")) cd = 0.28;
        else cd = 0.30; // Default

        double widthM = evData.getWidthMm() / 1000.0;
        double heightM = evData.getHeightMm() / 1000.0;

        return cd * (0.8 * widthM * heightM);
    }

    // ------------------------
    // Calcolo consumo energia per link
    // ------------------------
    private double calcModel(Link link, double travelTime, double linkEnterTime, EvModel evData){
        // ----------------------------
        // PARAMETRI FISICI
        // ----------------------------
        double massKg = calculateMassEstimate(evData);
        double cr     = calculateCrEstimate();
        double cda    = calculateCdaEstimate(evData);

        // ----------------------------
        // CINEMATICA
        // ----------------------------
        double length = link.getLength();
        double avgSpeed = length / travelTime;

        // Velocità effettiva per l’aerodinamica
        double effectiveSpeed = Math.max(avgSpeed, 8.0); // ~30 km/h

        double slopeRad = getSlopeAngleRadians(link);
        double sinA = Math.sin(slopeRad);
        double cosA = Math.cos(slopeRad);

        // ----------------------------
        // FORZE
        // ----------------------------
        double fRolling = massKg * GRAVITY * cr * cosA;

        double fAero = 0.5 * AIR_DENSITY * cda * effectiveSpeed * effectiveSpeed;

        // Aerodinamica fortemente ridotta in urbano
        if (avgSpeed < 8.3) { // < 30 km/h
            fAero *= 0.3;
        }

        double fSlope = massKg * GRAVITY * sinA;
        double fTractive = fRolling + fAero + fSlope;

        // ----------------------------
        // EFFICIENZA DIPENDENTE DALLA VELOCITÀ
        // ----------------------------
        double efficiency;
        if (avgSpeed < 8.0) {            // traffico urbano denso
            efficiency = 0.75;
        } else if (avgSpeed < 20.0) {    // urbano / extraurbano
            efficiency = 0.85;
        } else {                         // alte velocità
            efficiency = 0.80;
        }

        // ----------------------------
        // LAVORO MECCANICO
        // ----------------------------
        double workJoules = fTractive * length;

        // ----------------------------
        // STOP & GO ESPLICITO (TRAFFICO)
        // ----------------------------
        double freeFlowTime = length / link.getFreespeed();
        double congestionFactor = travelTime / freeFlowTime;

        // Limitiamo l’effetto per evitare esplosioni
        double stopGoFactor = Math.min(1.2, congestionFactor);
        workJoules *= stopGoFactor;

        double totalEnergy;

        if (workJoules >= 0) {
            // Consumo in trazione
            totalEnergy = workJoules / efficiency;
        } else {
            // Rigenerazione con limite di potenza
            double regenEnergy = workJoules * REGEN_EFFICIENCY;
            double maxRegenEnergy = MAX_REGEN_POWER_W * travelTime;
            totalEnergy = Math.max(regenEnergy, -maxRegenEnergy);
        }

        log.debug("EV {} link {} → {:.1f} J (v={:.1f} m/s, cong={:.2f})",
                electricVehicle.getId(),
                link.getId(),
                totalEnergy,
                avgSpeed,
                congestionFactor);

        return totalEnergy;
    }

    @Override
    public double calcEnergyConsumption(Link link, double travelTime, double linkEnterTime) {
        Double energyJoules = 0.0;
        // Recupero dati EV dal fleet manager
        EvModel evData = fleetManager.getVehicle(electricVehicle.getId());
        if (evData == null) {
            log.warn("Veicolo {} non trovato, consumo impostato a 0", electricVehicle.getId());
        }else if(evData.getState().equals(EvModel.State.MOVING)){
            energyJoules = calcModel(link, travelTime, linkEnterTime, evData);
            // Aggiornamento distanza percorsa dal veicolo
            evData.addDistanceTraveled(link.getLength());
        }
        return energyJoules;
    }
}
