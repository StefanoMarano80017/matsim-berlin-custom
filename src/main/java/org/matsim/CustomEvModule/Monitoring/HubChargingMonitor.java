package org.matsim.CustomEvModule.Monitoring;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.ServerEvSetup.SimulationInterface.SimulationBridgeInterface;
import org.matsim.contrib.ev.charging.ChargingEndEvent;
import org.matsim.contrib.ev.charging.ChargingEndEventHandler;
import org.matsim.contrib.ev.charging.ChargingStartEvent;
import org.matsim.contrib.ev.charging.ChargingStartEventHandler;

public class HubChargingMonitor implements ChargingStartEventHandler, ChargingEndEventHandler {

    private static final Logger log = LogManager.getLogger(HubChargingMonitor.class);

    private final SimulationBridgeInterface simulationBridgeInterface;

    public HubChargingMonitor(SimulationBridgeInterface simulationBridgeInterface) {
        this.simulationBridgeInterface = simulationBridgeInterface;
    }

    @Override
    public void handleEvent(ChargingStartEvent event) {
        simulationBridgeInterface.handleChargingStart(event.getChargerId(), event.getVehicleId(), event.getTime());
        logChargingStart(event);
    }

    @Override
    public void handleEvent(ChargingEndEvent event) {
        simulationBridgeInterface.handleChargingEnd(event.getChargerId(), event.getCharge());
        logChargingEnd(event);
    }

    private void logChargingStart(ChargingStartEvent event) {
        log.info("[HubChargingMonitor] [START] time={} vehicle={} charger={}",
                event.getTime(),
                event.getVehicleId(),
                event.getChargerId()
        );
    }

    private void logChargingEnd(ChargingEndEvent event) {
        log.info("[HubChargingMonitor] [END] time={} vehicle={} charger={} energy={}",
                event.getTime(),
                event.getVehicleId(),
                event.getChargerId(),
                event.getCharge()
        );
    }
}
