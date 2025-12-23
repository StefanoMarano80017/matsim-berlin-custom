package org.matsim.CustomEvModule.Monitoring;

import org.matsim.CustomEvModule.ChargingHub.HubManager;
import org.matsim.contrib.ev.charging.ChargingEndEvent;
import org.matsim.contrib.ev.charging.ChargingEndEventHandler;
import org.matsim.contrib.ev.charging.ChargingStartEvent;
import org.matsim.contrib.ev.charging.ChargingStartEventHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class HubChargingMonitor implements ChargingStartEventHandler, ChargingEndEventHandler {

    private static final Logger log = LogManager.getLogger(HubChargingMonitor.class);

    private final HubManager hubManager;

    public HubChargingMonitor(
        HubManager hubManager
    ) {
        this.hubManager = hubManager;
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
    }

}
