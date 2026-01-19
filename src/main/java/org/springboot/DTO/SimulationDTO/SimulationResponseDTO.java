package org.springboot.DTO.SimulationDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO strutturato per le risposte di controllo della simulazione
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResponseDTO {
    private String state;           // IDLE, RUNNING, COMPLETED, ERROR, STOPPED
    private String message;         // Messaggio descrittivo
    private String error;           // Errore se presente (null se tutto ok)
    private Long timestamp;         // Timestamp della risposta

    public SimulationResponseDTO(String state, String message) {
        this.state = state;
        this.message = message;
        this.error = null;
        this.timestamp = System.currentTimeMillis();
    }

    public SimulationResponseDTO(String state, String message, String error) {
        this.state = state;
        this.message = message;
        this.error = error;
        this.timestamp = System.currentTimeMillis();
    }

    public static SimulationResponseDTO success(String state, String message) {
        return new SimulationResponseDTO(state, message);
    }

    public static SimulationResponseDTO error(String state, String message, String errorDetails) {
        return new SimulationResponseDTO(state, message, errorDetails);
    }
}
