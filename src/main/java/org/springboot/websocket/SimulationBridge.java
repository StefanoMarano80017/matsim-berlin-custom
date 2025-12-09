package org.springboot.websocket;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springboot.DTO.WebSocketUpdate;
import org.springboot.DTO.payload.HubStatusPayload;
import org.springboot.DTO.payload.HubUpdatePayload;
import org.springboot.DTO.payload.SimpleTextPayload;
import org.springboot.DTO.payload.VehicleStatus;
import org.springboot.DTO.payload.VehicleUpdatePayload;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

@Component
public class SimulationBridge implements SimulationEventPublisher {

    private final SimulationWebSocketHandler wsHandler;

    public SimulationBridge(SimulationWebSocketHandler wsHandler) {
        this.wsHandler = wsHandler;
    }

    public void VehicleStatusPublish(List<VehicleStatus> stats, Double simTimestamp){
        VehicleUpdatePayload payload = new VehicleUpdatePayload(simTimestamp, stats);
        publish(
            new WebSocketUpdate<VehicleUpdatePayload>(
                "TimeStepUpdate",
                0.0,
                payload,
                "success"
            )
        );
    }

    public void HubStatusPublish(Map<String, Integer> HubOccupancyMap, Map<String, Double> HubEnergyMap, Double simTimeStamp){
        List<HubStatusPayload> hubStatuses = HubEnergyMap.entrySet().stream()
            .map(entry -> new HubStatusPayload(
                    entry.getKey(),
                    entry.getValue(),
                    HubOccupancyMap.getOrDefault(entry.getKey(), 0)
            ))
            .collect(Collectors.toList());

        HubUpdatePayload stepPayload = new HubUpdatePayload(simTimeStamp, hubStatuses);
        publish(
            new WebSocketUpdate<HubUpdatePayload>(
                "TimeStepUpdate",
                0,
                stepPayload,
                "success"
            )
        );
    }

    public void SimpleTextPublish(String Text){
        SimpleTextPayload EndSimMsg = new SimpleTextPayload(Text);
        publish(
            new WebSocketUpdate<SimpleTextPayload>( 
                "EndSim",
                1.0,
                EndSimMsg,
                "success"
            )
        );
    }

    @Override
    public <T> void publish(WebSocketUpdate<T> update){
        try {
            wsHandler.broadcast(toJson(update));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private <T> String toJson(WebSocketUpdate<T> update) {
        return new Gson().toJson(update); // oppure Jackson
    }
}
