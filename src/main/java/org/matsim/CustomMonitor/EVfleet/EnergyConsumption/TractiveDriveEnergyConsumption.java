package org.matsim.CustomMonitor.EVfleet.EnergyConsumption;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.CustomMonitor.EVfleet.EvFleetManager;
import org.matsim.CustomMonitor.model.EvModel;
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
 */
public class TractiveDriveEnergyConsumption implements DriveEnergyConsumption {

    private static final Logger log = LogManager.getLogger(TractiveDriveEnergyConsumption.class);

    // Costanti fisiche
    private static final double GRAVITY = 9.81;
    private static final double AIR_DENSITY = 1.225;

    // Rigenerazione realistica
    private static final double REGEN_EFFICIENCY = 0.55;
    private static final double MAX_REGEN_POWER_W = 50_000; // 50 kW

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
        final double BASE_MASS_KG = 1450.0;
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

    private double calculateCrEstimate() {
        return 0.010;
    }

    private double calculateCdaEstimate(EvModel evData) {
        double cd;
        String type = evData.getCarBodyType().toLowerCase();

        if (type.contains("suv") || type.contains("van")) {
            cd = 0.35;
        } else if (type.contains("hatchback") || type.contains("crossover")) {
            cd = 0.32;
        } else {
            cd = 0.28;
        }

        double widthM = evData.getWidthMm() / 1000.0;
        double heightM = evData.getHeightMm() / 1000.0;

        return cd * (0.8 * widthM * heightM);
    }

    @Override
    public double calcEnergyConsumption(Link link, double travelTime, double linkEnterTime) {

        EvModel evData = fleetManager.getVehicle(electricVehicle.getId());
        if (evData == null) {
            return 0.0;
        }

        // ----------------------------
        // PARAMETRI FISICI
        // ----------------------------
        double massKg = calculateMassEstimate(evData);
        double cr = calculateCrEstimate();
        double cda = calculateCdaEstimate(evData);

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

        // ----------------------------
        // CONSUMI AUSILIARI
        // ----------------------------
        double auxPower = 1000.0; // 1 kW costante
        totalEnergy += auxPower * travelTime;

        // ----------------------------
        // AGGIORNAMENTI
        // ----------------------------
        evData.addDistanceTraveled(length);

        log.debug("EV {} link {} → {:.1f} J (v={:.1f} m/s, cong={:.2f})",
                electricVehicle.getId(),
                link.getId(),
                totalEnergy,
                avgSpeed,
                congestionFactor);

        return totalEnergy;
    }
}
