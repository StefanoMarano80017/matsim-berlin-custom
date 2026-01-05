package org.springboot.DTO.WebSocketDTO.payload;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "payload aggiornamento hub e veicoli ad ogni timestep via WebSocket")
public class TimeStepPayload {

    private Double timestamp;

    @Schema(description = "List dei veicoli con i loro stati")
    private List<VehicleStatus> vehicles;

    @Schema(description = "List di hub con i loro stati")
    private List<HubStatusPayload> hubs;

    // Costruttore vuoto
    public TimeStepPayload() {
    }

    // Costruttore completo
    public TimeStepPayload(Double timestamp, List<VehicleStatus> vehicles, List<HubStatusPayload> hubs) {
        this.timestamp = timestamp;
        this.vehicles = vehicles;
        this.hubs = hubs;
    }

    // Getter e setter
    public Double getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Double timestamp) {
        this.timestamp = timestamp;
    }

    public List<VehicleStatus> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<VehicleStatus> vehicles) {
        this.vehicles = vehicles;
    }

    public List<HubStatusPayload> getHubs() {
        return hubs;
    }

    public void setHubs(List<HubStatusPayload> hubs) {
        this.hubs = hubs;
    }

    // toString()
    @Override
    public String toString() {
        return "TimeStepPayload{" +
                "timestamp=" + timestamp +
                ", vehicles=" + vehicles +
                ", hubs=" + hubs +
                '}';
    }
}
