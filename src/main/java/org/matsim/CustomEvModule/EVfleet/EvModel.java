package org.matsim.CustomEvModule.EVfleet;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;

/**
 * Modello di veicolo elettrico completo, con dati statici e dinamici,
 * aggiornato per supportare "dirty flags" per ottimizzare gli aggiornamenti WebSocket.
 */
public class EvModel {
    // --- Dati statici ---
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
    private final int lengthMm;
    private final int widthMm;
    private final int heightMm;
    private final String carBodyType;

    // --- Dati dinamici ---
    private double currentSoc;           
    private double currentEnergyJoules; 
    
    private double lastSoc;
    private double lastEnergyJoules;

    private double distanceTraveledKm;   
    private State state;                 
    private String linkId;        
    private double coordX;
    private double coordY;       

    // --- Dirty flag per delta WebSocket ---
    private boolean dirty = false;

    // Enum interno per lo stato del veicolo
    public enum State {
        MOVING,
        STOPPED,
        CHARGING,
        PARKED,
        IDLE
    }

    // --- Costruttore ---
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
        this.state = State.STOPPED;
        this.dirty = true; // segnala come modificato inizialmente
        this.lastSoc = currentSoc;
        this.lastEnergyJoules = currentEnergyJoules;
    }

    // --- Aggiornamento dinamico ---
    public void updateDynamicState(double soc, double energyJoules) {
        if (this.currentSoc != soc || this.currentEnergyJoules != energyJoules) {
            /*
            * Salvo il vecchio così posso calcolare il delta e quindi l'erogazione
            */
            this.lastSoc = currentSoc;
            this.lastEnergyJoules = currentEnergyJoules;

            this.currentSoc = soc;
            this.currentEnergyJoules = energyJoules;
            this.dirty = true;
        }
    }

    public void addDistanceTraveled(double distanceMeters) {
        if (distanceMeters != 0.0) {
            this.distanceTraveledKm += (distanceMeters / 1000.0);
            this.dirty = true;
        }
    }

    public void setState(State state) {
        if (this.state != state) {
            this.state = state;
            this.dirty = true;
        }
    }

    public void setLinkId(String linkId) {
        if (this.linkId == null || !this.linkId.equals(linkId)) {
            this.linkId = linkId;
            this.dirty = true;
        }
    }

    public void setCoord(double x, double y) {
        if (this.coordX != x || this.coordY != y) {
            this.coordX = x;
            this.coordY = y;
            this.dirty = true;
        }
    }

    // --- Dirty flag ---
    public boolean isDirty() {
        return dirty;
    }

    public void resetDirty() {
        this.dirty = false;
    }

    // --- Getter ---
    public Id<Vehicle> getVehicleId()           { return vehicleId; }
    public String getManufacturer()             { return manufacturer; }
    public String getModel()                    { return model; }
    public double getNominalCapacityKwh()       { return nominalCapacityKwh; }
    public double getConsumptionKwhPerKm()      { return consumptionKwhPerKm; }
    public String getBatteryType()              { return batteryType; }
    public int getNumberOfCells()               { return numberOfCells; }
    public double getTorqueNm()                 { return torqueNm; }
    public double getTopSpeedKmh()              { return topSpeedKmh; }
    public double getRangeKm()                  { return rangeKm; }
    public double getAcceleration0To100()       { return acceleration0To100; }
    public double getFastChargingPowerKwDc()    { return fastChargingPowerKwDc; }
    public String getFastChargePort()           { return fastChargePort; }
    public double getTowingCapacityKg()         { return towingCapacityKg; }
    public double getCargoVolumeL()             { return cargoVolumeL; }
    public int getSeats()                       { return seats; }
    public String getDrivetrain()               { return drivetrain; }
    public String getSegment()                  { return segment; }
    public int getLengthMm()                    { return lengthMm; }
    public int getWidthMm()                     { return widthMm; }
    public int getHeightMm()                    { return heightMm; }
    public String getCarBodyType()              { return carBodyType; }
    public double getCurrentSoc()               { return currentSoc; }
    public double getCurrentEnergyJoules()      { return currentEnergyJoules; }
    public double getLastSoc()                  { return lastSoc;}
    public double getLastEnergyJoules()         { return lastEnergyJoules;}
    public double getDistanceTraveledKm()       { return distanceTraveledKm; }
    public State getState()                     { return state; }
    public String getLinkId()                   { return linkId; }
    public double getCoordX()                   { return coordX; }
    public double getCoordY()                   { return coordY; }

    // --- Autonomia stimata ---
    public double getEstimatedRemainingRangeKm() {
        if (consumptionKwhPerKm <= 0) return 0.0;
        double currentEnergyKwh = currentEnergyJoules / 3.6e6;
        return currentEnergyKwh / consumptionKwhPerKm;
    }

    // --- Backward compatibility ---
    public boolean isCharging() {
        return this.state == State.CHARGING;
    }

    @Deprecated
    public void setCharging() {
        setState(State.CHARGING);
    }
}
