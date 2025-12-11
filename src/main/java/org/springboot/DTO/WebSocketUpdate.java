package org.springboot.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Classe base generica per tutti i messaggi di aggiornamento su WebSocket.
 *
 * @param <T> Il tipo di dato specifico contenuto nel payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Messaggio aggiornamento via WebSocket ha un campo payload generico da adattare in base al messaggio")
public class WebSocketUpdate<T> {

    // Identifica il tipo di messaggio (es. "SIMULATION_UPDATE", "ERROR")
    @Schema(description = "Tipo messaggio")
    private String type; 

    // Campo per il progresso o lo stato
    @Schema(description = "Progresso simulazione")
    private double progress;

    // Un payload di dati generico che contiene l'oggetto specifico dell'aggiornamento.
    @Schema(description = "Payload personalizzabile")
    private T payload; 

    // Un messaggio di stato o diagnostico opzionale.
    @Schema(description = "Messaggio di stato opzionale")
    private String statusMessage;
}