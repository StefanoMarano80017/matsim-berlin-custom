package org.springboot.DTO.out.WebSocketDTO;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Classe base generica per tutti i messaggi di aggiornamento su WebSocket.
 *
 * @param <T> Il tipo di dato specifico contenuto nel payload.
 */
@Schema(description = "Messaggio aggiornamento via WebSocket ha un campo payload generico da adattare in base al messaggio")
public class WebSocketUpdate<T> {

    @Schema(description = "Tipo messaggio")
    private String type; 

    @Schema(description = "Messaggio di stato opzionale")
    private String statusMessage;

    @Schema(description = "Payload personalizzabile")
    private T payload; 

    // Costruttore vuoto
    public WebSocketUpdate() {}

    // Costruttore completo
    public WebSocketUpdate(String type, T payload, String statusMessage) {
        this.type = type;
        this.payload = payload;
        this.statusMessage = statusMessage;
    }

    // Getter e setter
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    @Override
    public String toString() {
        return "WebSocketUpdate{" +
                "type='" + type + '\'' +
                ", payload=" + payload +
                ", statusMessage='" + statusMessage + '\'' +
                '}';
    }
}
