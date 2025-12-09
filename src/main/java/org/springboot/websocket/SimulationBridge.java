package org.springboot.websocket;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springboot.DTO.WebSocketUpdate;
import org.springboot.DTO.payload.HubStatusPayload;
import org.springboot.DTO.payload.HubUpdatePayload;
import org.springboot.DTO.payload.SimpleTextPayload;
import org.springboot.DTO.payload.TimeStepPayload;
import org.springboot.DTO.payload.VehicleStatus;
import org.springboot.DTO.payload.VehicleUpdatePayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

@Component
public class SimulationBridge implements SimulationEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(SimulationBridge.class);

    private final SimulationWebSocketHandler wsHandler;
    private final Gson gson;

    public SimulationBridge(SimulationWebSocketHandler wsHandler) {
        this.wsHandler = wsHandler;
        this.gson = new Gson();
    }

    // ----------------- PUBLIC METHODS -----------------
    public void publishTimeStep(List<VehicleStatus> vehicleStatuses, 
                                Map<String, Integer> hubOccupancyMap, 
                                Map<String, Double> hubEnergyMap, 
                                Double simTimestamp) {

        logger.debug("Publishing TimeStep at timestamp: {}", simTimestamp);

        List<HubStatusPayload> hubStatuses = buildHubStatusPayload(hubOccupancyMap, hubEnergyMap);
        TimeStepPayload payload = new TimeStepPayload(simTimestamp, vehicleStatuses, hubStatuses);

        publishWebSocketUpdate("TimeStepUpdate", 0.0, payload);
    }

    public void publishVehicleStatus(List<VehicleStatus> stats, Double simTimestamp) {
        logger.debug("Publishing VehicleStatus at timestamp: {}", simTimestamp);

        VehicleUpdatePayload payload = new VehicleUpdatePayload(simTimestamp, stats);
        publishWebSocketUpdate("TimeStepUpdate_Vehicle", 0.0, payload);
    }

    public void publishHubStatus(Map<String, Integer> hubOccupancyMap, Map<String, Double> hubEnergyMap, Double simTimestamp) {
        logger.debug("Publishing HubStatus at timestamp: {}", simTimestamp);

        List<HubStatusPayload> hubStatuses = buildHubStatusPayload(hubOccupancyMap, hubEnergyMap);
        HubUpdatePayload payload = new HubUpdatePayload(simTimestamp, hubStatuses);

        publishWebSocketUpdate("TimeStepUpdate_Hub", 0.0, payload);
    }

    public void publishSimpleText(String text) {
        logger.debug("Publishing SimpleText: {}", text);

        SimpleTextPayload payload = new SimpleTextPayload(text);
        publishWebSocketUpdate("EndSim", 1.0, payload);
    }

    // ----------------- PRIVATE HELPERS -----------------
    private List<HubStatusPayload> buildHubStatusPayload(Map<String, Integer> hubOccupancyMap, Map<String, Double> hubEnergyMap) {
        return hubEnergyMap.entrySet().stream()
            .map(entry -> new HubStatusPayload(
                    entry.getKey(),
                    entry.getValue(),
                    hubOccupancyMap.getOrDefault(entry.getKey(), 0)
            ))
            .collect(Collectors.toList());
    }

    private <T> void publishWebSocketUpdate(String type, Double progress, T payload) {
        WebSocketUpdate<T> update = new WebSocketUpdate<>(type, progress, payload, "success");
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