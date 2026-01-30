package org.springboot.DTO.out.WebSocketDTO.payload;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "payload aggiornamento hub e veicoli ad ogni timestep via WebSocket")
public final class TimeStepPayload {

    private Double timestamp;
    private String formattedTime;

    @Schema(description = "List dei veicoli con i loro stati")
    private final List<VehicleStatus> vehicles;

    @Schema(description = "List di hub con i loro stati")
    private final List<HubStatusPayload> hubs;

    // Costruttore completo
    public TimeStepPayload(
        Double timestamp, 
        List<VehicleStatus> vehicles, 
        List<HubStatusPayload> hubs
    ) {
        this.timestamp      = timestamp;
        this.formattedTime  = FormattedTime();
        this.vehicles       = List.copyOf(vehicles);
        this.hubs           = List.copyOf(hubs);
    }

    // Getter e setter
    public Double getTimestamp() {
        return timestamp;
    }

    public String getFormattedTime(){
        return formattedTime;
    }

    public void setTimestamp(Double timestamp) {
        this.timestamp = timestamp;
        this.formattedTime = FormattedTime();
    }

    public List<VehicleStatus> getVehicles() {
        return vehicles;
    }

    public List<HubStatusPayload> getHubs() {
        return hubs;
    }

    public String FormattedTime() {
        if (timestamp == null) return "00:00:00";

        long totalSeconds = Math.max(0, timestamp.longValue());

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
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
