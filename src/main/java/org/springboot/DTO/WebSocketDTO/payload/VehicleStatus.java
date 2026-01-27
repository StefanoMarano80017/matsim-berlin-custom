package org.springboot.DTO.WebSocketDTO.payload;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "payload aggiornamento vehicle con stato e link via WebSocket")
public class VehicleStatus {

    private String vehicleId;              // riferimento al veicolo
    private double soc;                    // stato di carica in %
    private double kmDriven;               // chilometri percorsi
    private double currentEnergyJoules;    // energia corrente in Joule
    private String State;                  // stato del veicolo
    private String linkId;                 // opzionale: link corrente, valido se MOVING o CHARGING
    private Double simTime;                // opzionale: tempo di simulazione relativo al link

    // Costruttore vuoto
    public VehicleStatus() {}

    // Costruttore completo senza link (per STOPPED o PARKED)
    public VehicleStatus(
        String vehicleId,
        double soc,
        double kmDriven,
        double currentEnergyJoules,
        String State
    ) {
        this.vehicleId = vehicleId;
        this.soc = soc;
        this.kmDriven = kmDriven;
        this.currentEnergyJoules = currentEnergyJoules;
        this.State = State;
        this.linkId = null;
        this.simTime = null;
    }

    // Costruttore completo con link (per MOVING o CHARGING)
    public VehicleStatus(
        String vehicleId,
        double soc,
        double kmDriven,
        double currentEnergyJoules,
        String State,
        String linkId,
        Double simTime
    ) {
        this.vehicleId = vehicleId;
        this.soc = soc;
        this.kmDriven = kmDriven;
        this.currentEnergyJoules = currentEnergyJoules;
        this.State = State;

        // Link valido solo se MOVING o CHARGING
        if(State.contains("moving") || State.contains("charging")){
            this.linkId = linkId;
            this.simTime = simTime;
        } else {
            this.linkId = null;
            this.simTime = null;
        }
    }

    // --- Getters e Setters ---
    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public double getSoc() { return soc; }
    public void setSoc(double soc) { this.soc = soc; }

    public double getKmDriven() { return kmDriven; }
    public void setKmDriven(double kmDriven) { this.kmDriven = kmDriven; }

    public double getCurrentEnergyJoules() { return currentEnergyJoules; }
    public void setCurrentEnergyJoules(double currentEnergyJoules) { this.currentEnergyJoules = currentEnergyJoules; }

    public String getState() { return State; }
    public void setState(String String) { this.State = String; }

    public String getLinkId() { return linkId; }
    public void setLinkId(String linkId) {
        if(State.contains("moving") || State.contains("charging")){
            this.linkId = linkId;
        } else {
            this.linkId = null;
        }
    }

    public Double getSimTime() { return simTime; }
    public void setSimTime(Double simTime) {
        if(State.contains("moving") || State.contains("charging")){
            this.simTime = simTime;
        } else {
            this.simTime = null;
        }
    }

    // --- Backward compatibility ---
    public boolean isCharging() {
        return (this.State.contains("charging"));
    }

    @Override
    public String toString() {
        return "VehicleStatus{" +
                "vehicleId='" + vehicleId + '\'' +
                ", soc=" + soc +
                ", kmDriven=" + kmDriven +
                ", currentEnergyJoules=" + currentEnergyJoules +
                ", State=" + State +
                ", linkId='" + linkId + '\'' +
                ", simTime=" + simTime +
                '}';
    }
}
