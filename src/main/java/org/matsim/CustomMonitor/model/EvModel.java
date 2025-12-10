package org.matsim.CustomMonitor.model;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;

/**
 * Modello di veicolo elettrico completo, con dati statici e dinamici.
 */
public class EvModel {
    // --- Dati statici (specifiche del veicolo) ---
    private final Id<Vehicle> vehicleId;
    private final String manufacturer;
    private final String model;
    private final double nominalCapacityKwh;
    private final double consumptionKwhPerKm;
    private final String batteryType;
    private final int    numberOfCells;
    private final double torqueNm;
    private final double topSpeedKmh;
    private final double rangeKm;
    private final double acceleration0To100;
    private final double fastChargingPowerKwDc;
    private final String fastChargePort;
    private final double towingCapacityKg;
    private final double cargoVolumeL;
    private final int    seats;
    private final String drivetrain;
    private final String segment;
    private final int    lengthMm;
    private final int    widthMm;
    private final int    heightMm;
    private final String carBodyType;
    // --- Dati dinamici (stato di simulazione) ---
    private double  currentSoc;           // 0.0-1.0
    private double  currentEnergyJoules;  // Livello corrente in Joule
    private double  distanceTraveledKm;   // Distanza percorsa
    private boolean isCharging;           // True se in ricarica

    public EvModel(
            Id<Vehicle> vehicleId,
            String manufacturer,
            String model,
            double nominalCapacityKwh,
            double consumptionKwhPerKm,
            String batteryType,
            int numberOfCells,
            double torqueNm,
            double topSpeedKmh,
            double rangeKm,
            double acceleration0To100,
            double fastChargingPowerKwDc,
            String fastChargePort,
            double towingCapacityKg,
            double cargoVolumeL,
            int seats,
            String drivetrain,
            String segment,
            int lengthMm,
            int widthMm,
            int heightMm,
            String carBodyType
    ) {
        this.vehicleId = vehicleId;
        this.manufacturer = manufacturer;
        this.model = model;
        this.nominalCapacityKwh = nominalCapacityKwh;
        this.consumptionKwhPerKm = consumptionKwhPerKm;
        this.batteryType = batteryType;
        this.numberOfCells = numberOfCells;
        this.torqueNm = torqueNm;
        this.topSpeedKmh = topSpeedKmh;
        this.rangeKm = rangeKm;
        this.acceleration0To100 = acceleration0To100;
        this.fastChargingPowerKwDc = fastChargingPowerKwDc;
        this.fastChargePort = fastChargePort;
        this.towingCapacityKg = towingCapacityKg;
        this.cargoVolumeL = cargoVolumeL;
        this.seats = seats;
        this.drivetrain = drivetrain;
        this.segment = segment;
        this.lengthMm = lengthMm;
        this.widthMm = widthMm;
        this.heightMm = heightMm;
        this.carBodyType = carBodyType;

        // inizializzazione dinamica
        this.currentSoc = 1.0;
        this.currentEnergyJoules = nominalCapacityKwh * 3.6e6;
        this.distanceTraveledKm = 0.0;
        this.isCharging = false;
    }

    // --- Aggiornamento dinamico ---
    public void updateDynamicState(double soc, double energyJoules) {
        this.currentSoc = soc;
        this.currentEnergyJoules = energyJoules;
    }

    public void addDistanceTraveled(double distanceMeters) {
        this.distanceTraveledKm += (distanceMeters / 1000.0);
    }

    // --- Getter ---
    public Id<Vehicle>  getVehicleId() { return vehicleId; }
    public String       getManufacturer() { return manufacturer; }
    public String       getModel() { return model; }
    public double       getNominalCapacityKwh() { return nominalCapacityKwh; }
    public double       getConsumptionKwhPerKm() { return consumptionKwhPerKm; }
    public String       getBatteryType() { return batteryType; }
    public int          getNumberOfCells() { return numberOfCells; }
    public double       getTorqueNm() { return torqueNm; }
    public double       getTopSpeedKmh() { return topSpeedKmh; }
    public double       getRangeKm() { return rangeKm; }
    public double       getAcceleration0To100() { return acceleration0To100; }
    public double       getFastChargingPowerKwDc() { return fastChargingPowerKwDc; }
    public String       getFastChargePort() { return fastChargePort; }
    public double       getTowingCapacityKg() { return towingCapacityKg; }
    public double       getCargoVolumeL() { return cargoVolumeL; }
    public int          getSeats() { return seats; }
    public String       getDrivetrain() { return drivetrain; }
    public String       getSegment() { return segment; }
    public int          getLengthMm() { return lengthMm; }
    public int          getWidthMm() { return widthMm; }
    public int          getHeightMm() { return heightMm; }
    public String       getCarBodyType() { return carBodyType; }
    public double       getCurrentSoc() { return currentSoc; }
    public double       getCurrentEnergyJoules() { return currentEnergyJoules; }
    public double       getDistanceTraveledKm() { return distanceTraveledKm; }
    public boolean      isCharging() { return isCharging; }


    // ---- Setter ---
    public void setCharging(boolean charging) { this.isCharging = charging; }


    /**
     * Autonomia stimata residua (km) basata su SOC attuale
     */
    public double getEstimatedRemainingRangeKm() {
        if (consumptionKwhPerKm <= 0) return 0.0;
        double currentEnergyKwh = currentEnergyJoules / 3.6e6;
        return currentEnergyKwh / consumptionKwhPerKm;
    }
}
