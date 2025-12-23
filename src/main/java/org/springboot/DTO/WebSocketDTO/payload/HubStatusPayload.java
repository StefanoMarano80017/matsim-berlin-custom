package org.springboot.DTO.WebSocketDTO.payload;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "payload aggiornamento hub via WebSocket")
public class HubStatusPayload {

    private String hubId;
    private double energy;       // energia totale consumata
    private int occupancy;       // numero di veicoli in carica

    // Costruttore vuoto
    public HubStatusPayload() {
    }

    // Costruttore completo
    public HubStatusPayload(String hubId, double energy, int occupancy) {
        this.hubId = hubId;
        this.energy = energy;
        this.occupancy = occupancy;
    }

    // Getter e setter
    public String getHubId() {
        return hubId;
    }

    public void setHubId(String hubId) {
        this.hubId = hubId;
    }

    public double getEnergy() {
        return energy;
    }

    public void setEnergy(double energy) {
        this.energy = energy;
    }

    public int getOccupancy() {
        return occupancy;
    }

    public void setOccupancy(int occupancy) {
        this.occupancy = occupancy;
    }

    // Optional: toString()
    @Override
    public String toString() {
        return "HubStatusPayload{" +
                "hubId='" + hubId + '\'' +
                ", energy=" + energy +
                ", occupancy=" + occupancy +
                '}';
    }
}
