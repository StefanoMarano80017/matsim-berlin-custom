package org.springboot.DTO.payload;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "payload aggiornamento hub e veicoli ad ogni timestep via WebSocket")
public class TimeStepPayload {
    private Double timestamp;
    @Schema(description = "List dei veicoli con i loro stati")
    private List<VehicleStatus> vehicles;
    @Schema(description = "List di hub con i loro stati")
    private List<HubStatusPayload> hubs;
}
