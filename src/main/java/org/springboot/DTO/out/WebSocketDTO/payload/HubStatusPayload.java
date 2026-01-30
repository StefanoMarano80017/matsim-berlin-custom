package org.springboot.DTO.out.WebSocketDTO.payload;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.Map;

@Schema(description = "payload aggiornamento hub via WebSocket")
public class HubStatusPayload {

    private String hubId;
    private double energy;                          // energia totale consumata dall’hub
    private int occupancy;                          // numero di veicoli in carica
    private Map<String, ChargerStatus> chargers;    // stato dei charger
    private ArrayList<Double> position;             // [lat, lon] della posizione dell'hub

    public HubStatusPayload() {}

    public HubStatusPayload(
        String hubId, 
        double energy, 
        int occupancy, 
        Map<String, ChargerStatus> chargers,
        ArrayList<Double> position
    ) {
        this.hubId = hubId;
        this.energy = energy;
        this.occupancy = occupancy;
        this.chargers = chargers;
        this.position = position;
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

    public ArrayList<Double> getPosition() {
        return position;
    }

    public void setPosition(ArrayList<Double> position) {
        this.position = position;
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
