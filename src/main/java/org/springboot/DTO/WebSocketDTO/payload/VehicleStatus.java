package org.springboot.DTO.WebSocketDTO.payload;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "payload aggiornamento vehicle via WebSocket")
public class VehicleStatus {

    private String vehicleId;        // riferimento al veicolo
    private double soc;              // stato di carica in %
    private double kmDriven;         // chilometri percorsi
    private double currentEnergyJoules;
    private boolean isCharging;      // es: "moving", "stopped", "charging"

    // Costruttore vuoto
    public VehicleStatus() {
    }

    // Costruttore completo
    public VehicleStatus(String vehicleId, double soc, double kmDriven, double currentEnergyJoules, boolean isCharging) {
        this.vehicleId = vehicleId;
        this.soc = soc;
        this.kmDriven = kmDriven;
        this.currentEnergyJoules = currentEnergyJoules;
        this.isCharging = isCharging;
    }

    // Getter e setter
    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public double getSoc() {
        return soc;
    }

    public void setSoc(double soc) {
        this.soc = soc;
    }

    public double getKmDriven() {
        return kmDriven;
    }

    public void setKmDriven(double kmDriven) {
        this.kmDriven = kmDriven;
    }

    public double getCurrentEnergyJoules() {
        return currentEnergyJoules;
    }

    public void setCurrentEnergyJoules(double currentEnergyJoules) {
        this.currentEnergyJoules = currentEnergyJoules;
    }

    public boolean isCharging() {
        return isCharging;
    }

    public void setCharging(boolean charging) {
        isCharging = charging;
    }

    // toString()
    @Override
    public String toString() {
        return "VehicleStatus{" +
                "vehicleId='" + vehicleId + '\'' +
                ", soc=" + soc +
                ", kmDriven=" + kmDriven +
                ", currentEnergyJoules=" + currentEnergyJoules +
                ", isCharging=" + isCharging +
                '}';
    }
}
