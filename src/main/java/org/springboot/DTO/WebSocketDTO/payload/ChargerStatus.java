package org.springboot.DTO.WebSocketDTO.payload;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Stato di un singolo charger in un hub")
public class ChargerStatus {

    private String chargerId;
    private boolean occupied;
    private double energy;

    public ChargerStatus() {
    }

    public ChargerStatus(String chargerId, boolean occupied, double energy) {
        this.chargerId = chargerId;
        this.occupied = occupied;
        this.energy = energy;
    }

    public String getChargerId() {
        return chargerId;
    }

    public void setChargerId(String chargerId) {
        this.chargerId = chargerId;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }

    public double getEnergy() {
        return energy;
    }

    public void setEnergy(double energy) {
        this.energy = energy;
    }

    @Override
    public String toString() {
        return "ChargerStatus{" +
                "chargerId='" + chargerId + '\'' +
                ", occupied=" + occupied +
                ", energy=" + energy +
                '}';
    }
}
