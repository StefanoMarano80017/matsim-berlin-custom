package org.matsim.CustomMonitor.EVfleet.EnergyConsumption;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.CustomMonitor.EVfleet.EvFleetManager;
import org.matsim.CustomMonitor.model.EvModel;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.fleet.ElectricVehicle;

/**
 * Modello di consumo basato sulle forze di trazione (rolling, aerodinamica, pendenza),
 * calibrato per simulazioni MATSim (stop&go implicito, velocità medie, rigenerazione).
 *
 * Obiettivo:
 * - evitare sovrastima del consumo
 * - mantenere coerenza fisica
 * - produrre valori realistici (kWh/100km)
 */
public class TractiveDriveEnergyConsumption implements DriveEnergyConsumption {

    private static final Logger log = LogManager.getLogger(TractiveDriveEnergyConsumption.class);

    // Costanti fisiche
    private static final double GRAVITY = 9.81;        // m/s^2
    private static final double AIR_DENSITY = 1.225;  // kg/m^3

    private final ElectricVehicle electricVehicle;
    private final EvFleetManager fleetManager;

    // Efficienza massima di rigenerazione (realistica per EV)
    private static final double REGEN_EFFICIENCY = 0.55;

    public TractiveDriveEnergyConsumption(ElectricVehicle electricVehicle, EvFleetManager fleetManager) {
        this.electricVehicle = electricVehicle;
        this.fleetManager = fleetManager;
    }

    /**
     * Recupera la pendenza del link.
     * Supporta:
     * - percentuale (es. 5 = 5%)
     * - gradi
     *
     * Usa atan() per evitare errori grossolani sulla forza di gravità.
     */
    private double getSlopeAngleRadians(Link link) {
        try {
            Double slope = (Double) link.getAttributes().getAttribute("slope");
            if (slope != null) {
                // Se è troppo grande per essere un angolo in radianti,
                // assumiamo che sia percentuale
                if (Math.abs(slope) > 0.3) {
                    slope = slope / 100.0;
                }
                return Math.atan(slope);
            }
        } catch (Exception e) {
            // fallback a pendenza zero
        }
        return 0.0;
    }

    /**
     * Stima della massa del veicolo.
     *
     * Scelte:
     * - peso batteria 5 kg/kWh (valore realistico EV moderni)
     * - correzione moderata per segmento
     */
    private double calculateMassEstimate(EvModel evData) {
        final double BASE_MASS_KG = 1450.0;
        final double BATTERY_KG_PER_KWH = 5.0;

        double mass = BASE_MASS_KG
                + evData.getNominalCapacityKwh() * BATTERY_KG_PER_KWH;

        if (evData.getSegment().equalsIgnoreCase("SUV")) {
            mass += 200.0;
        } else if (evData.getSegment().equalsIgnoreCase("Hatchback")) {
            mass -= 100.0;
        }
        return mass;
    }

    /**
     * Coefficiente di resistenza al rotolamento.
     * Valore medio realistico per pneumatici stradali EV.
     */
    private double calculateCrEstimate() {
        return 0.010;
    }

    /**
     * Stima CdA (coefficiente aerodinamico * area frontale).
     * Approccio volutamente semplice ma stabile.
     */
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

        // 80% del rettangolo → area frontale
        double area = 0.8 * widthM * heightM;

        return cd * area;
    }

    @Override
    public double calcEnergyConsumption(Link link, double travelTime, double linkEnterTime) {

        EvModel evData = fleetManager.getVehicle(electricVehicle.getId());
        if (evData == null) {
            log.warn("Veicolo {} non trovato, consumo impostato a 0",
                    electricVehicle.getId());
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

        // Evita che velocità medie basse annullino l’aerodinamica
        double effectiveSpeed = Math.max(avgSpeed, 8.0); // ~30 km/h

        double slopeRad = getSlopeAngleRadians(link);
        double sinA = Math.sin(slopeRad);
        double cosA = Math.cos(slopeRad);

        // ----------------------------
        // FORZE
        // ----------------------------
        double fRolling = massKg * GRAVITY * cr * cosA;
        double fAero = 0.5 * AIR_DENSITY * cda * effectiveSpeed * effectiveSpeed;
        double fSlope = massKg * GRAVITY * sinA;

        double fTractive = fRolling + fAero + fSlope;

        // Efficienza powertrain:
        // - più bassa in trazione (carichi elevati)
        // - più alta in rilascio
        double tractionEfficiency = fTractive > 0 ? 0.85 : 0.90;

        // ----------------------------
        // LAVORO MECCANICO
        // ----------------------------
        double workJoules = fTractive * length;

        /**
         * Correzione stop&go:
         * MATSim usa velocità media → sovrastima il lavoro sulle resistenze.
         * Questo fattore riporta il consumo su valori osservati.
         */
        if (avgSpeed < 13.9) { // < 50 km/h
            workJoules *= 0.70;
        }

        double totalEnergy;

        if (workJoules >= 0) {
            // Trazione
            totalEnergy = workJoules / tractionEfficiency;
        } else {
            // Rigenerazione (limitata)
            totalEnergy = workJoules * REGEN_EFFICIENCY;
        }

        // ----------------------------
        // CONSUMI AUSILIARI
        // ----------------------------
        // HVAC + elettronica di bordo (~1 kW)
        double auxPower = 1000.0;
        totalEnergy += auxPower * travelTime;

        // ----------------------------
        // AGGIORNAMENTI
        // ----------------------------
        evData.addDistanceTraveled(length);

        log.debug("EV {}, link {} → consumo {:.1f} J (v={:.1f} m/s)",
                electricVehicle.getId(),
                link.getId(),
                totalEnergy,
                avgSpeed);

        return totalEnergy;
    }
}
