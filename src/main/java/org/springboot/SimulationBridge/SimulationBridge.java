package org.springboot.SimulationBridge;

import org.springboot.DTO.WebSocketDTO.WebSocketUpdate;
import org.springboot.DTO.WebSocketDTO.payload.HubStatusPayload;
import org.springboot.DTO.WebSocketDTO.payload.HubUpdatePayload;
import org.springboot.DTO.WebSocketDTO.payload.SimpleTextPayload;
import org.springboot.DTO.WebSocketDTO.payload.TimeStepPayload;
import org.springboot.DTO.WebSocketDTO.payload.VehicleStatus;
import org.springboot.DTO.WebSocketDTO.payload.VehicleUpdatePayload;
import org.springboot.websocket.SimulationEventPublisher;
import org.springboot.websocket.SimulationWebSocketHandler;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import jakarta.annotation.PostConstruct;

@Component
public class SimulationBridge implements SimulationEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(SimulationBridge.class);

    private final SimulationWebSocketHandler wsHandler;
    private final Gson gson;

    public SimulationBridge(SimulationWebSocketHandler wsHandler) {
        this.wsHandler = wsHandler;
        this.gson = new GsonBuilder()
            .registerTypeAdapter(
                Double.class,
                (JsonSerializer<Double>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(Math.round(src * 100) / 100.0)
            )
            .create();
    }

    // =====================================================
    // EVENT BUS SUBSCRIPTION
    // =====================================================
    @PostConstruct
    public void init() {
        SimulationEventBus.getInstance().subscribe(event -> {
            switch (event) {
                case TimeStepPayload timeStep ->
                        handleTimeStep(timeStep);

                case VehicleUpdatePayload vehicleUpdate ->
                        handleVehicleUpdate(vehicleUpdate);

                case HubUpdatePayload hubUpdate ->
                        handleHubUpdate(hubUpdate);

                case SimpleTextPayload simpleText ->
                        handleSimpleText(simpleText);

                default ->
                    logger.warn("Evento non gestito sul bus: {}", event.getClass().getSimpleName());
            }
        });
    }

    // ========= TIMESTEP =========
    // ========= EVENT BUS =========
    public void handleTimeStep(TimeStepPayload payload) {
        publishWsTimeStep(payload);
    }

    // ========= SPRING (PAYLOAD GIÀ PRONTO) =========
    public void publishWsTimeStep(TimeStepPayload payload) {
        logger.debug("Publishing TimeStep at timestamp: {}", payload.getTimestamp());
        publishWebSocketUpdate("TimeStepUpdate", 0.0, payload);
    }

    // ========= SPRING (DATI GREZZI) =========
    public void publishWsTimeStep(
            Double simTimestamp,
            List<VehicleStatus> vehicles,
            Map<String, Integer> hubOccupancyMap,
            Map<String, Double> hubEnergyMap
    ) {

        List<HubStatusPayload> hubStatuses =
            buildHubStatusPayload(hubOccupancyMap, hubEnergyMap);

        TimeStepPayload payload =
            new TimeStepPayload(simTimestamp, vehicles, hubStatuses);

        publishWsTimeStep(payload);
    }

    // ========= VEHICLE UPDATE ===========
    // ========= EVENT BUS =========
    public void handleVehicleUpdate(VehicleUpdatePayload payload) {
        publishWsVehicleUpdate(payload);
    }

    // ========= SPRING (PAYLOAD GIÀ PRONTO) =========
    public void publishWsVehicleUpdate(VehicleUpdatePayload payload) {
        publishWebSocketUpdate("TimeStepUpdate_Vehicle", 0.0, payload);
    }

    // ========= SPRING (DATI GREZZI) =========
    public void publishWsVehicleUpdate(Double simTimestamp, List<VehicleStatus> vehicles) {
        VehicleUpdatePayload payload = new VehicleUpdatePayload(simTimestamp, vehicles);
        publishWsVehicleUpdate(payload);
    }


    // ========= HUB UPDATE ===========
    // ========= EVENT BUS =========
    public void handleHubUpdate(HubUpdatePayload payload) {
        publishWsHubUpdate(payload);
    }

    // ========= SPRING (PAYLOAD GIÀ PRONTO) =========
    public void publishWsHubUpdate(HubUpdatePayload payload) {
        publishWebSocketUpdate("TimeStepUpdate_Hub", 0.0, payload);
    }

    // ========= SPRING (DATI GREZZI) =========
    public void publishWsHubUpdate(
            Double simTimestamp,
            Map<String, Integer> hubOccupancyMap,
            Map<String, Double> hubEnergyMap
    ) {
        List<HubStatusPayload> hubStatuses = buildHubStatusPayload(hubOccupancyMap, hubEnergyMap);
        HubUpdatePayload payload = new HubUpdatePayload(simTimestamp, hubStatuses);
        publishWsHubUpdate(payload);
    }

    // ========= TEXT UPDATE ===========
    // ========= EVENT BUS =========
    public void handleSimpleText(SimpleTextPayload payload) {
        publishWsSimpleText(payload);
    }

    // ========= SPRING (PAYLOAD GIÀ PRONTO) =========
    public void publishWsSimpleText(SimpleTextPayload payload) {
        publishWebSocketUpdate("SimpleNotification", 1.0, payload);
    }

    // ========= SPRING (DATI GREZZI) =========
    public void publishWsSimpleText(String text) {
        publishWsSimpleText(new SimpleTextPayload(text));
    }

    // =========== UTILITY ===========
    private List<HubStatusPayload> buildHubStatusPayload(Map<String, Integer> hubOccupancyMap, Map<String, Double> hubEnergyMap) {
        return hubEnergyMap.entrySet().stream()
            .map(entry -> new HubStatusPayload(
                entry.getKey(),
                entry.getValue(),
                hubOccupancyMap.getOrDefault(entry.getKey(), 0)
            ))
            .collect(Collectors.toList());
    }


    // =====================================================
    // LOW-LEVEL WS
    // =====================================================
    private <T> void publishWebSocketUpdate(String type, Double progress, T payload) {
        WebSocketUpdate<T> update =
            new WebSocketUpdate<>(type, progress, payload, "success");
        publish(update);
    }

    @Override
    public <T> void publish(WebSocketUpdate<T> update) {
        try {
            String json = gson.toJson(update);
            logger.debug("Broadcasting WebSocketUpdate: {}", json);
            wsHandler.broadcast(json);
        } catch (Exception e) {
            logger.error("Error while broadcasting update", e);
        }
    }
}
