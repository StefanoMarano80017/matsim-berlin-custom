package org.matsim.CustomMonitor.EVfleet;

import java.util.Set;

import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;

public class QuickLinkDebugHandler implements LinkEnterEventHandler {

    private final Set<Id<Vehicle>> electricVehicleIds;

    public QuickLinkDebugHandler(Set<Id<Vehicle>> electricVehicleIds) {
        this.electricVehicleIds = electricVehicleIds;
    }


    @Override
    public void handleEvent(LinkEnterEvent event) {
        if (electricVehicleIds.contains(event.getVehicleId())) {
            System.out.printf("Veicolo %s entra link %s al tempo %.0f%n",
                    event.getVehicleId(),
                    event.getLinkId(),
                    event.getTime());
        }


        if(event.getVehicleId().toString().equals("EV_0_car")) {
            System.out.printf("Veicolo %s entra link %s al tempo %.0f%n",
                    event.getVehicleId(),
                    event.getLinkId(),
                    event.getTime());
        }
    }
}
