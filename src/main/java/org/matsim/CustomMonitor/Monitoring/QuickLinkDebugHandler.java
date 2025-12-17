package org.matsim.CustomMonitor.Monitoring;

import java.util.Set;

import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;


public class QuickLinkDebugHandler implements LinkEnterEventHandler, VehicleEntersTrafficEventHandler {

    private static final Logger log = LogManager.getLogger(QuickLinkDebugHandler.class);
    private final Set<Id<Vehicle>> electricVehicleIds;

    public QuickLinkDebugHandler(Set<Id<Vehicle>> electricVehicleIds) {
        this.electricVehicleIds = electricVehicleIds;
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        if (electricVehicleIds.contains(event.getVehicleId())) {
            log.info("Veicolo {} entra link {} al tempo {}", 
                event.getVehicleId(), 
                event.getLinkId(), 
                event.getTime());
        }
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        if (event.getVehicleId().toString().startsWith("EV_")) {
            log.info("Veicolo {} ha iniziato il viaggio sul link {} al tempo {}", 
                event.getVehicleId(), 
                event.getLinkId(), 
                event.getTime());
        }
    }
}
