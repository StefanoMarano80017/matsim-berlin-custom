package org.matsim.CustomMonitor.EVfleet.EnergyConsumption;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.CustomMonitor.EVfleet.EvFleetManager;
import org.matsim.CustomMonitor.model.EvModel;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.fleet.ElectricVehicle;

/**
 * Modello di consumo basato sulla trazione (Tractive Energy Model), che simula
 * l'energia necessaria per superare le forze di resistenza: rotolamento, aerodinamica, pendenza.
 *
 * Utilizza la velocità media sul link e parametri fisici stimati dal EvModel.
 * NOTA: Richiede che l'attributo "slope" (pendenza in gradi) sia eventualmente disponibile sul Link.
 */
public class TractiveDriveEnergyConsumption implements DriveEnergyConsumption {

    private static final Logger log = LogManager.getLogger(TractiveDriveEnergyConsumption.class);

    // Costanti fisiche
    private static final double GRAVITY = 9.81;       // Accelerazione di gravità (m/s^2)
    private static final double AIR_DENSITY = 1.225;  // Densità dell'aria (kg/m^3)
    // Efficienze
    private static final double EFFICIENCY_MOTOR = 0.9;       // Efficienza motore
    private static final double REGEN_EFFICIENCY = 0.75;      // Efficienza rigenerativa
    // Veicolo e fleet manager
    private final ElectricVehicle electricVehicle;
    private final EvFleetManager fleetManager;

    public TractiveDriveEnergyConsumption(ElectricVehicle electricVehicle, EvFleetManager fleetManager) {
        this.electricVehicle = electricVehicle;
        this.fleetManager = fleetManager;
    }

    // ------------------------
    // Metodi di stima dei parametri fisici
    // ------------------------

    /**
     * Recupera la pendenza del link e la converte in radianti.
     * Se l'attributo non esiste o non è un numero, assume pendenza 0.
     */
    private double getSlopeRadians(Link link) {
        Object slopeAttr = link.getAttributes().getAttribute("slope");
        if (slopeAttr instanceof Number) {
            return Math.toRadians(((Number) slopeAttr).doubleValue());
        }
        return 0.0;
    }

    /**
     * Stima la massa totale del veicolo (kg) in base a:
     * - Massa base
     * - Capacità batteria
     * - Segmento del veicolo (SUV, Hatchback, etc.)
     */
    private double estimateMass(EvModel ev) {
        double mass = 1500.0 + 7.0 * ev.getNominalCapacityKwh(); // Massa base + batteria
        switch (ev.getSegment().toLowerCase()) {
            case "suv": mass += 300; break;
            case "hatchback": mass -= 100; break;
        }
        return mass;
    }

    /**
     * Stima il coefficiente di resistenza al rotolamento Cr -> assumo 0.01 perchè ci metti dei pneumatici belli su un auto nuova 
     */
    private double estimateCr() { return 0.01; }

    /**
     * Stima il prodotto Cd*A (coefficiente aerodinamico * area frontale) in m^2
     * basato sul tipo di carrozzeria e dimensioni del veicolo
     */
    private double estimateCdA(EvModel ev) {
        String body = ev.getCarBodyType().toLowerCase();
        double cd;

        // Stima Cd in base al tipo di carrozzeria
        if (body.contains("suv") || body.contains("van") || body.contains("wagon") || body.contains("estate")) cd = 0.35;
        else if (body.contains("hatchback") || body.contains("crossover")) cd = 0.32;
        else if (body.contains("sedan") || body.contains("coupe") || body.contains("cabriolet") || body.contains("liftback")) cd = 0.28;
        else cd = 0.30; // Default

        // Area frontale: 80% del rettangolo W x H (approssimazione)
        double area = 0.8 * (ev.getWidthMm() / 1000.0) * (ev.getHeightMm() / 1000.0);
        return cd * area;
    }

    // ------------------------
    // Calcolo consumo energia per link
    // ------------------------
    private double calcModel(Link link, double travelTime, double linkEnterTime, EvModel evData){
        // --- Parametri fisici stimati ---
        double mass  = estimateMass(evData);
        double cr    = estimateCr();
        double cdA   = estimateCdA(evData);
        double slope = getSlopeRadians(link);
        // --- Variabili di simulazione ---
        double linkLength = link.getLength();
        double v          = linkLength / travelTime;  // velocità media m/s
        double cosAlpha   = Math.cos(slope);
        double sinAlpha   = Math.sin(slope);
        // --- Calcolo forze di resistenza (Newton) ---
        double fRolling = mass * GRAVITY * cr * cosAlpha;    // Rotolamento
        double fAero    = 0.5 * AIR_DENSITY * cdA * v * v;   // Aerodinamica
        double fSlope   = mass * GRAVITY * sinAlpha;         // Pendenza
        double fTractive = fRolling + fAero + fSlope;
        // --- Calcolo lavoro/energia (Joule) ---
        double workJoules = fTractive * linkLength;
        double energyJoules;
        if (workJoules >= 0) {
            // Energia richiesta dal motore (trazione)
            energyJoules = workJoules / EFFICIENCY_MOTOR;
        } else {
            // Rigenerazione in discesa
            energyJoules = workJoules * REGEN_EFFICIENCY;
        }

        return energyJoules;
    }

    @Override
    public double calcEnergyConsumption(Link link, double travelTime, double linkEnterTime) {
        Double energyJoules = 0.0;
        // Recupero dati EV dal fleet manager
        EvModel evData = fleetManager.getVehicle(electricVehicle.getId());
        if (evData == null) {
            log.warn("Veicolo {} non trovato, consumo impostato a 0", electricVehicle.getId());
            return 0.0;
        }else if(evData.getState().equals(EvModel.State.MOVING)){
            energyJoules = calcModel(link, travelTime, linkEnterTime, evData);
            // Aggiornamento distanza percorsa dal veicolo
            evData.addDistanceTraveled(link.getLength());
            log.debug("Veicolo {}, Link {} - Consumo Tractive: {} J, Velocità media: {} m/s",
                electricVehicle.getId(), link.getId(), energyJoules, link.getLength()/travelTime);
        }
        return energyJoules;
    }
}
