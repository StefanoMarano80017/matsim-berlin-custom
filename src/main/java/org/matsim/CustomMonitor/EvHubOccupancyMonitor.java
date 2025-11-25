package org.matsim.CustomMonitor;

import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import java.util.HashSet;
import java.util.Set;

/**
 * Monitora l'occupazione dell'Hub di ricarica reagendo agli eventi di 
 * inizio e fine attività di ricarica ("charging").
 */
public class EvHubOccupancyMonitor implements ActivityStartEventHandler, ActivityEndEventHandler {

    private int vehiclesInHub = 0;
    private final String CHARGING_ACT_TYPE = "charging";

    // Set per tenere traccia dei nostri ID specifici (come definiti in EVGenerator)
    private final Set<String> myFleetIds = new HashSet<>();

    public EvHubOccupancyMonitor() {
        // Popoliamo gli ID che ci interessano (i 5 EV generati)
        for(int i=0; i<5; i++) myFleetIds.add("EV_Driver_" + i);
    }

    /**
     * Helper per formattare il tempo come H:M:S
     */
    private String formatTime(double seconds) {
        int h = (int) (seconds / 3600);
        int m = (int) ((seconds % 3600) / 60);
        int s = (int) (seconds % 60);
        return String.format("%02d:%02d:%02d", h, m, s);
    }
    
    // Gestisce l'ingresso nell'attività di ricarica
    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (event.getActType().equals(CHARGING_ACT_TYPE)) {
            if (myFleetIds.contains(event.getPersonId().toString())) {
                vehiclesInHub++;
                System.out.println("T=" + formatTime(event.getTime()) + "s: ENTRATA Ricarica (" + event.getPersonId() + "). Auto in Hub: " + vehiclesInHub);
            }
        }
    }

    // Gestisce l'uscita dall'attività di ricarica
    @Override
    public void handleEvent(ActivityEndEvent event) {
        if (event.getActType().equals(CHARGING_ACT_TYPE)) {
            if (myFleetIds.contains(event.getPersonId().toString())) {
                vehiclesInHub--;
                System.out.println("T=" + formatTime(event.getTime()) + "s: USCITA Ricarica (" + event.getPersonId() + "). Auto in Hub: " + vehiclesInHub);
            }
        }
    }
}