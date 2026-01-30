package org.springboot.DTO.out.SimulationDTO;

import java.io.Serializable;

public class EvDto implements Serializable {

    // --- statici ---
    private String vehicleId;
    private String manufacturer;
    private String model;
    private double nominalCapacityKwh;
    private double consumptionKwhPerKm;
    private String batteryType;
    private int numberOfCells;
    private double torqueNm;
    private double topSpeedKmh;
    private double rangeKm;
    private double acceleration0To100;
    private double fastChargingPowerKwDc;
    private String fastChargePort;
    private double towingCapacityKg;
    private double cargoVolumeL;
    private int seats;
    private String drivetrain;
    private String segment;
    private int lengthMm;
    private int widthMm;
    private int heightMm;
    private String carBodyType;

    // --- dinamici ---
    private double currentSoc;
    private double currentEnergyJoules;
    private double distanceTraveledKm;
    private String state;
    private String linkId;

    // --- Getter & Setter ---

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public double getNominalCapacityKwh() {
        return nominalCapacityKwh;
    }

    public void setNominalCapacityKwh(double nominalCapacityKwh) {
        this.nominalCapacityKwh = nominalCapacityKwh;
    }

    public double getConsumptionKwhPerKm() {
        return consumptionKwhPerKm;
    }

    public void setConsumptionKwhPerKm(double consumptionKwhPerKm) {
        this.consumptionKwhPerKm = consumptionKwhPerKm;
    }

    public String getBatteryType() {
        return batteryType;
    }

    public void setBatteryType(String batteryType) {
        this.batteryType = batteryType;
    }

    public int getNumberOfCells() {
        return numberOfCells;
    }

    public void setNumberOfCells(int numberOfCells) {
        this.numberOfCells = numberOfCells;
    }

    public double getTorqueNm() {
        return torqueNm;
    }

    public void setTorqueNm(double torqueNm) {
        this.torqueNm = torqueNm;
    }

    public double getTopSpeedKmh() {
        return topSpeedKmh;
    }

    public void setTopSpeedKmh(double topSpeedKmh) {
        this.topSpeedKmh = topSpeedKmh;
    }

    public double getRangeKm() {
        return rangeKm;
    }

    public void setRangeKm(double rangeKm) {
        this.rangeKm = rangeKm;
    }

    public double getAcceleration0To100() {
        return acceleration0To100;
    }

    public void setAcceleration0To100(double acceleration0To100) {
        this.acceleration0To100 = acceleration0To100;
    }

    public double getFastChargingPowerKwDc() {
        return fastChargingPowerKwDc;
    }

    public void setFastChargingPowerKwDc(double fastChargingPowerKwDc) {
        this.fastChargingPowerKwDc = fastChargingPowerKwDc;
    }

    public String getFastChargePort() {
        return fastChargePort;
    }

    public void setFastChargePort(String fastChargePort) {
        this.fastChargePort = fastChargePort;
    }

    public double getTowingCapacityKg() {
        return towingCapacityKg;
    }

    public void setTowingCapacityKg(double towingCapacityKg) {
        this.towingCapacityKg = towingCapacityKg;
    }

    public double getCargoVolumeL() {
        return cargoVolumeL;
    }

    public void setCargoVolumeL(double cargoVolumeL) {
        this.cargoVolumeL = cargoVolumeL;
    }

    public int getSeats() {
        return seats;
    }

    public void setSeats(int seats) {
        this.seats = seats;
    }

    public String getDrivetrain() {
        return drivetrain;
    }

    public void setDrivetrain(String drivetrain) {
        this.drivetrain = drivetrain;
    }

    public String getSegment() {
        return segment;
    }

    public void setSegment(String segment) {
        this.segment = segment;
    }

    public int getLengthMm() {
        return lengthMm;
    }

    public void setLengthMm(int lengthMm) {
        this.lengthMm = lengthMm;
    }

    public int getWidthMm() {
        return widthMm;
    }

    public void setWidthMm(int widthMm) {
        this.widthMm = widthMm;
    }

    public int getHeightMm() {
        return heightMm;
    }

    public void setHeightMm(int heightMm) {
        this.heightMm = heightMm;
    }

    public String getCarBodyType() {
        return carBodyType;
    }

    public void setCarBodyType(String carBodyType) {
        this.carBodyType = carBodyType;
    }

    public double getCurrentSoc() {
        return currentSoc;
    }

    public void setCurrentSoc(double currentSoc) {
        this.currentSoc = currentSoc;
    }

    public double getCurrentEnergyJoules() {
        return currentEnergyJoules;
    }

    public void setCurrentEnergyJoules(double currentEnergyJoules) {
        this.currentEnergyJoules = currentEnergyJoules;
    }

    public double getDistanceTraveledKm() {
        return distanceTraveledKm;
    }

    public void setDistanceTraveledKm(double distanceTraveledKm) {
        this.distanceTraveledKm = distanceTraveledKm;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getLinkId() {
        return linkId;
    }

    public void setLinkId(String linkId) {
        this.linkId = linkId;
    }
}
