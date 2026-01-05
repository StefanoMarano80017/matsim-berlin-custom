package org.springboot.websocket;

import org.apache.poi.ss.formula.functions.T;
import org.springboot.DTO.WebSocketDTO.WebSocketUpdate;

public interface SimulationEventPublisher {
    /**
     * Metodo generico per pubblicare qualsiasi messaggio WebSocketUpdate.
     * * Il <T> tra 'public' e 'void' dichiara T come un parametro di tipo 
     * utilizzabile solo all'interno di questo metodo.
     *
     * @param <T> Il tipo di payload (es. SimpleTextPayload, SimulationPayload, ecc.).
     * @param message L'oggetto WebSocketUpdate contenente il payload specifico.
     */
    @SuppressWarnings("hiding")
    public <T> void publish(WebSocketUpdate<T> message);
}
