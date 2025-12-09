package org.springboot.DTO.payload;

import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "payload aggiornamento hub via WebSocket")
public class HubStatusPayload {
    private String hubId;
    private double energy;       // energia totale consumata
    private int occupancy;       // numero di veicoli in carica
}
