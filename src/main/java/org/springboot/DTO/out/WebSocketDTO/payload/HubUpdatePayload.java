package org.springboot.DTO.out.WebSocketDTO.payload;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "payload complessivo aggiornamento hub via WebSocket")
public class HubUpdatePayload {

    private Double timestamp;
    
    @Schema(description = "List di hub con i loro stati")
    private List<HubStatusPayload> hubs;

    // Costruttore vuoto
    public HubUpdatePayload() {
    }

    // Costruttore completo
    public HubUpdatePayload(Double timestamp, List<HubStatusPayload> hubs) {
        this.timestamp = timestamp;
        this.hubs = hubs;
    }

    // Getter e setter
    public Double getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Double timestamp) {
        this.timestamp = timestamp;
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
        return "HubUpdatePayload{" +
                "timestamp=" + timestamp +
                ", hubs=" + hubs +
                '}';
    }
}
