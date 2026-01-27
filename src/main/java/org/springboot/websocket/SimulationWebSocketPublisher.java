package org.springboot.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springboot.DTO.out.WebSocketDTO.WebSocketUpdate;
import org.springboot.DTO.out.WebSocketDTO.payload.TimeStepPayload;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

/**
 * Responsabilità: Pubblicazione dati via WebSocket.
 * 
 * Riceve dati già estratti e li invia via WebSocket.
 * Gestisce la serializzazione JSON e il broadcast.
 * 
 * Questa classe NON conosce della simulazione,
 * si limita a inviare dati via WebSocket.
 */
@Component
public class SimulationWebSocketPublisher {

    private static final Logger logger = LoggerFactory.getLogger(SimulationWebSocketPublisher.class);
    private final Gson gson;
    private final SimulationWebSocketHandler wsHandler;

    public SimulationWebSocketPublisher(SimulationWebSocketHandler wsHandler) {
        this.wsHandler = wsHandler;
        // Configura Gson per arrotondare i Double a 2 decimali
        this.gson = new GsonBuilder().registerTypeAdapter(
                Double.class,
                    (JsonSerializer<Double>) (src, typeOfSrc, context) -> new JsonPrimitive(Math.round(src * 100) / 100.0)
                ).create();
    }

    /**
     * Pubblica uno snapshot TimeStep via WebSocket.
     * 
     * @param payload Il TimeStepPayload con i dati della simulazione
     * @return true se la pubblicazione è riuscita, false altrimenti
     */
    public boolean publishTimeStepSnapshot(TimeStepPayload payload) {
        if (payload == null) {
            return false;
        }

        try {
            publishWebSocketUpdate("TimeStepUpdate", payload);
            return true;
        } catch (Exception e) {
            logger.error("Errore durante publishTimeStepSnapshot", e);
            return false;
        }
    }

    /**
     * Pubblica un messaggio generico via WebSocket.
     * 
     * @param message Il messaggio da inviare (es. "SIMULATION_START", "SIMULATION_END")
     */
    public void publishMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }

        try {
            publishWebSocketUpdate("SimulationMessage", message);
        } catch (Exception e) {
            logger.error("Errore durante publishMessage", e);
        }
    }

    /**
     * Metodo privato: invia un aggiornamento generico via WebSocket.
     */
    private <T> void publishWebSocketUpdate(String type, T payload) {
        WebSocketUpdate<T> update = new WebSocketUpdate<>(type, payload, "success");
        try {
            wsHandler.broadcast(gson.toJson(update));
        } catch (Exception e) {
            logger.error("Errore broadcast WebSocket", e);
        }
    }
}
