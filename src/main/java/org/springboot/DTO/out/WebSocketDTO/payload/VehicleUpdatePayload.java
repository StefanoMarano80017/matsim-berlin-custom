package org.springboot.DTO.out.WebSocketDTO.payload;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "payload complessivo aggiornamento veicolo via WebSocket")
public class VehicleUpdatePayload {

    private Double timestamp;
    
    @Schema(description = "List di veicoli con i loro stati")
    private List<VehicleStatus> vehicles;

    // Costruttore vuoto
    public VehicleUpdatePayload() {
    }

    // Costruttore completo
    public VehicleUpdatePayload(Double timestamp, List<VehicleStatus> vehicles) {
        this.timestamp = timestamp;
        this.vehicles = vehicles;
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

    // toString()
    @Override
    public String toString() {
        return "VehicleUpdatePayload{" +
                "timestamp=" + timestamp +
                ", vehicles=" + vehicles +
                '}';
    }
}
