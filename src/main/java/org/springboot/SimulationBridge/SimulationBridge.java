package org.springboot.SimulationBridge;

import org.springboot.DTO.WebSocketDTO.WebSocketUpdate;
import org.springboot.DTO.WebSocketDTO.payload.TimeStepPayload;
import org.springboot.websocket.SimulationWebSocketHandler;
import org.matsim.ServerEvSetup.SimulationInterface.SimulationBridgeInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

@Component
public class SimulationBridge {

    private static final Logger logger = LoggerFactory.getLogger(SimulationBridge.class);

    private final Gson gson;
    private final SimulationWebSocketHandler wsHandler;

    public SimulationBridge(SimulationWebSocketHandler wsHandler) {
        this.wsHandler = wsHandler;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(
                        Double.class,
                        (JsonSerializer<Double>) (src, typeOfSrc, context) ->
                                new JsonPrimitive(Math.round(src * 100) / 100.0)
                ).create();
    }

    /**
     * Invia lo snapshot della simulazione via WebSocket.
     * @param simTimestamp   Timestamp simulazione
     * @param evFleetManager Manager veicoli
     * @param hubManager     Manager hub
     * @param fullSnapshot   true = invia tutto, false = solo delta dirty
     */
    public boolean publishSimulationSnapshot(
        SimulationBridgeInterface simulationBridgeInterface,
        double simTimestamp,
        boolean fullSnapshot
    ) {
        try {
            TimeStepPayload payload = simulationBridgeInterface.GetTimeStepStatus(fullSnapshot);
            if (payload != null) {
                payload.setTimestamp(simTimestamp);
                publishWebSocketUpdate("TimeStepUpdate", 0.0, payload);
                // Reset dirty flags dopo invio delta
                if (!fullSnapshot) {
                    simulationBridgeInterface.resetDirty();
                }
                return true;
            }
        } catch (Exception e) {
            logger.error("Errore durante publishSimulationSnapshot", e);
        }
        return false;
    }

    /**
     * Invia un messaggio generico alla simulazione tramite WebSocket.
     * Utile per eventi come inizio/fine simulazione.
     *
     * @param message il testo del messaggio da inviare
     */
    public void publishSimulationMessage(String message) {
        if (message == null || message.isBlank()) {
            return; // ignora messaggi vuoti
        }
        publishWebSocketUpdate("SimulationMessage", 0.0, message);
    }

    // --- LOW-LEVEL WS ---
    private <T> void publishWebSocketUpdate(String type, Double progress, T payload) {
        WebSocketUpdate<T> update = new WebSocketUpdate<>(type, progress, payload, "success");
        try {
            wsHandler.broadcast(gson.toJson(update));
        } catch (Exception e) {
            logger.error("Errore broadcast WebSocket", e);
        }
    }
}
