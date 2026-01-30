package org.springboot.DTO.out;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO strutturato per le risposte di controllo della simulazione
 * @param <T> Tipo generico opzionale per dati aggiuntivi
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResponseDTO<T> {
    private String state;    // IDLE, RUNNING, COMPLETED, ERROR, STOPPED
    private String message;  // Messaggio descrittivo
    private String error;    // Errore se presente (null se tutto ok)
    private Long timestamp;  // Timestamp della risposta
    private T data;          // Campo opzionale generico

    // Costruttore senza data
    public SimulationResponseDTO(String state, String message) {
        this.state = state;
        this.message = message;
        this.error = null;
        this.timestamp = System.currentTimeMillis();
        this.data = null;
    }

    // Costruttore senza data, con errore
    public SimulationResponseDTO(String state, String message, String error) {
        this.state = state;
        this.message = message;
        this.error = error;
        this.timestamp = System.currentTimeMillis();
        this.data = null;
    }

    // Costruttore con data
    public SimulationResponseDTO(String state, String message, T data) {
        this.state = state;
        this.message = message;
        this.error = null;
        this.timestamp = System.currentTimeMillis();
        this.data = data;
    }

    // Static factory per successo senza dati
    public static <T> SimulationResponseDTO<T> success(String message) {
        return new SimulationResponseDTO<>("SUCCESS", message);
    }

    // Static factory per successo con dati
    public static <T> SimulationResponseDTO<T> success(String message, T data) {
        return new SimulationResponseDTO<>("SUCCESS", message, data);
    }

    // Static factory per errore
    public static <T> SimulationResponseDTO<T> error(String message, String errorDetails) {
        return new SimulationResponseDTO<>("ERROR", message, errorDetails);
    }

}
