package org.matsim.CustomMonitor.EVfleet;

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
            log.info("Veicolo %s entra link %s al tempo %.0f%n",
                    event.getVehicleId(),
                    event.getLinkId(),
                    event.getTime());
        }
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        if (event.getVehicleId().toString().startsWith("EV_")) {
           log.info("Veicolo %s ha iniziato il viaggio sul link %s al tempo %.0f%n", 
                    event.getVehicleId(), 
                    event.getLinkId(),
                    event.getTime());
        }
    }
}
