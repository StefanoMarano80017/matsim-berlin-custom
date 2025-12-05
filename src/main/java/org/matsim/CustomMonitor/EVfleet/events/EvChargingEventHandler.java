package org.matsim.CustomMonitor.EVfleet.events;

import org.matsim.CustomMonitor.model.EvModel;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.charging.*;
import org.matsim.vehicles.Vehicle;

import java.util.Map;

public class EvChargingEventHandler implements ChargingStartEventHandler, ChargingEndEventHandler {

    private final Map<Id<Vehicle>, EvModel> fleet;

    public EvChargingEventHandler(Map<Id<Vehicle>, EvModel> fleet) {
        this.fleet = fleet;
    }

    @Override
    public void handleEvent(ChargingStartEvent event) {
        EvModel ev = fleet.get(event.getVehicleId());
        if (ev != null) {
            ev.setCharging(true);
        }
    }

    @Override
    public void handleEvent(ChargingEndEvent event) {
        EvModel ev = fleet.get(event.getVehicleId());
        if (ev != null) {
            ev.setCharging(false);
        }
    }

    @Override
    public void reset(int iteration) {
        // opzionale, ma richiesto dall'interfaccia EventHandler
    }
}
