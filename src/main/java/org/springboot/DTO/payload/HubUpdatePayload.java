package org.springboot.DTO.payload;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "payload complessivo aggiornamento hub via WebSocket")
public class HubUpdatePayload {
    private Double timestamp;
    @Schema(description = "List di hub con i loro stati")
    private List<HubStatusPayload> hubs;
}