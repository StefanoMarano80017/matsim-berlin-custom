package org.matsim.CustomMonitor.Monitoring;

import org.matsim.CustomMonitor.ChargingHub.HubManager;
import org.matsim.CustomMonitor.EVfleet.EvFleetManager;
import org.matsim.contrib.ev.charging.ChargingEndEvent;
import org.matsim.contrib.ev.charging.ChargingEndEventHandler;
import org.matsim.contrib.ev.charging.ChargingStartEvent;
import org.matsim.contrib.ev.charging.ChargingStartEventHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springboot.DTO.WebSocketDTO.payload.VehicleStatus;

import com.google.inject.name.Named;

import org.springboot.DTO.WebSocketDTO.payload.TimeStepPayload;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HubChargingMonitor implements ChargingStartEventHandler, ChargingEndEventHandler {

    private static final Logger log = LogManager.getLogger(HubChargingMonitor.class);

    private final HubManager hubManager;
    private final EvFleetManager evFleetManager;

    public HubChargingMonitor(
        HubManager hubManager, 
        EvFleetManager evFleetManager
    ) {
        this.hubManager = hubManager;
        this.evFleetManager = evFleetManager;
    }

    @Override
    public void handleEvent(ChargingStartEvent event) {
        String hubId = hubManager.getHubIdForCharger(event.getChargerId());
        hubManager.incrementOccupancy(hubId);
        hubManager.recordTimeline(event.getTime());
        log.info("[HubChargingMonitor] [%.0f] START charging: charger=%s, hub=%s, occupancy=%d",
                event.getTime(),
                event.getChargerId(),
                hubId,
                hubManager.getHubOccupancy(hubId));

        //publishChargingEvent(event.getTime());
    }

    @Override
    public void handleEvent(ChargingEndEvent event) {
        String hubId = hubManager.getHubIdForCharger(event.getChargerId());
        hubManager.decrementOccupancy(hubId);

        double energy = event.getCharge();
        hubManager.addChargerEnergy(event.getChargerId(), energy);
        hubManager.addHubEnergy(hubId, energy);
        hubManager.recordTimeline(event.getTime());

        log.info("[HubChargingMonitor] [%.0f] END charging: charger=%s, hub=%s, charge=%.2fJ, occupancy=%d, totalHubEnergy=%.2fJ",
                event.getTime(),
                event.getChargerId(),
                hubId,
                energy,
                hubManager.getHubOccupancy(hubId),
                hubManager.getHubEnergy(hubId));

        //publishChargingEvent(event.getTime());
    }

    /**
     * Pubblica lo stato attuale dei veicoli e hub come payload TimeStepPayload sul bus.
     */
    /* 
    private void publishChargingEvent(double simTime) {
        if (!publishOnSpring) return;

        try {
            List<VehicleStatus> vehicleStatuses =
                    evFleetManager.getFleet().values().stream()
                            .map(v -> new VehicleStatus(
                                    v.getVehicleId().toString(),
                                    v.getCurrentSoc(),
                                    v.getDistanceTraveledKm(),
                                    v.getCurrentEnergyJoules(),
                                    v.getState() != null ? v.getState().toString() : "IDLE"
                            ))
                            .collect(Collectors.toList());

            Map<String, Integer> hubOccupancyMap = hubManager.getHubOccupancyMap();
            Map<String, Double> hubEnergyMap = hubManager.getHubEnergyMap();

            TimeStepPayload payload = new TimeStepPayload(
                    simTime,
                    vehicleStatuses,
                    buildHubStatusPayload(hubOccupancyMap, hubEnergyMap)
            );

            publishEvent(payload);
        } catch (Exception e) {
            log.error("[HubChargingMonitor] Errore durante la pubblicazione su EventBus: {}", e.getMessage());
        }
    }
        */
}
