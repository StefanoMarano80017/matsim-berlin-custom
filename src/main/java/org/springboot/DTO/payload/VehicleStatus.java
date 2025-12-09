package org.springboot.DTO.payload;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "payload aggiornamento vehicle via WebSocket")
public class VehicleStatus {
    private String vehicleId;       // riferimento al veicolo
    private double soc;             // stato di carica in %
    private double kmDriven;        // chilometri percorsi
    private double currentEnergyJoules;           
    private boolean isCharging;          // es: "moving", "stopped", "charging"
}
