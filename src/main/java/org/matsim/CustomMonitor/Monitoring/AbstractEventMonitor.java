package org.matsim.CustomMonitor.Monitoring;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springboot.DTO.WebSocketDTO.payload.HubStatusPayload;
import org.springboot.SimulationBridge.SimulationEventBus;

public abstract class AbstractEventMonitor {

    protected static final Logger log = LogManager.getLogger(AbstractEventMonitor.class);

    protected final SimulationEventBus eventBus;
    protected final boolean publishOnSpring;

    public AbstractEventMonitor(boolean publishOnSpring) {
        this.publishOnSpring = publishOnSpring;
        if (publishOnSpring) {
            this.eventBus = SimulationEventBus.getInstance();
            log.info("[AbstractEventMonitor] EventBus abilitato.");
        } else {
            this.eventBus = null;
            log.info("[AbstractEventMonitor] EventBus disabilitato.");
        }
    }

    /**
     * Pubblica un payload sul bus se il flag è true.
     * @param payload il DTO da pubblicare
     */
    protected <T> void publishEvent(T payload) {
        if (publishOnSpring && eventBus != null) {
            try {
                eventBus.publish(payload);
                log.debug("[AbstractEventMonitor] Payload pubblicato: {}", payload);
            } catch (Exception e) {
                log.error("[AbstractEventMonitor] Errore durante la pubblicazione: {}", e.getMessage());
            }
        }
    }


    /*
    *   Metodi helper 
    */
    protected List<HubStatusPayload> buildHubStatusPayload(
        Map<String, Integer> hubOccupancyMap, 
        Map<String, Double> hubEnergyMap
    ) {
        if (hubEnergyMap == null || hubEnergyMap.isEmpty()) return Collections.emptyList();
        return hubEnergyMap.entrySet().stream()
                .map(entry -> new HubStatusPayload(
                        entry.getKey(),
                        entry.getValue(),
                        hubOccupancyMap.getOrDefault(entry.getKey(), 0)
                ))
                .collect(Collectors.toList());
    }
}
