package org.springboot.DTO.WebSocketDTO.payload;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "payload aggiornamento hub via WebSocket")
public class HubStatusPayload {

    private String hubId;
    private double energy;                          // energia totale consumata dall’hub
    private int occupancy;                          // numero di veicoli in carica
    private Map<String, ChargerStatus> chargers;    // stato dei charger

    public HubStatusPayload() {}

    public HubStatusPayload(
        String hubId, 
        double energy, 
        int occupancy, 
        Map<String, ChargerStatus> chargers
    ) {
        this.hubId = hubId;
        this.energy = energy;
        this.occupancy = occupancy;
        this.chargers = chargers;
    }

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

    public Map<String, ChargerStatus> getChargers() {
        return chargers;
    }

    public void setChargers(Map<String, ChargerStatus> chargers) {
        this.chargers = chargers;
    }

    @Override
    public String toString() {
        return "HubStatusPayload{" +
                "hubId='" + hubId + '\'' +
                ", energy=" + energy +
                ", occupancy=" + occupancy +
                ", chargers=" + chargers +
                '}';
    }
}
