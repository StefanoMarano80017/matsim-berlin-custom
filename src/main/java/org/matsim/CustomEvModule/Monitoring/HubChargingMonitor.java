package org.matsim.CustomEvModule.Monitoring;

import org.matsim.CustomEvModule.Hub.ChargingHub;
import org.matsim.CustomEvModule.Hub.HubManager;
import org.matsim.contrib.ev.charging.ChargingEndEvent;
import org.matsim.contrib.ev.charging.ChargingEndEventHandler;
import org.matsim.contrib.ev.charging.ChargingStartEvent;
import org.matsim.contrib.ev.charging.ChargingStartEventHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.stream.Collectors;

public class HubChargingMonitor implements ChargingStartEventHandler, ChargingEndEventHandler {

    private static final Logger log = LogManager.getLogger(HubChargingMonitor.class);

    private final HubManager hubManager;

    public HubChargingMonitor(HubManager hubManager) {
        this.hubManager = hubManager;
    }

    @Override
    public void handleEvent(ChargingStartEvent event) {
        ChargingHub hub = hubManager.getHub(hubManager.getHubIdForCharger(event.getChargerId()));
        hub.incrementOccupancy(event.getChargerId(), 0.0); // energia a start=0
        logEvent("START", event.getVehicleId().toString(), event.getChargerId().toString(), hub, event.getTime(), 0.0);
    }

    @Override
    public void handleEvent(ChargingEndEvent event) {
        ChargingHub hub = hubManager.getHub(hubManager.getHubIdForCharger(event.getChargerId()));
        double energy = event.getCharge();
        hub.decrementOccupancy(event.getChargerId(), energy);
        logEvent("END", event.getVehicleId().toString(), event.getChargerId().toString(), hub, event.getTime(), energy);
    }

    private void logEvent(String type, String vehicleId, String chargerId, ChargingHub hub, double time, double energy) {
        String occupiedChargers = hub.getOccupiedChargers()
                                     .stream()
                                     .map(Id -> Id.toString())
                                     .collect(Collectors.joining(", "));
        log.info("[HubChargingMonitor] [{}] time={} vehicle={} charger={} hub={} occupancy={} totalHubEnergy={} occupiedChargers={}",
                type,
                time,
                vehicleId,
                chargerId,
                hub.getId(),
                hub.getOccupancy(),
                hub.getTotalEnergy(),
                occupiedChargers
        );
    }
}
