package org.matsim.CustomMonitor.Monitoring;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;

public class ActivityEventMonitor  implements ActivityStartEventHandler, ActivityEndEventHandler{
    
    private static final Logger log = LogManager.getLogger(ActivityEventMonitor.class);

    @Override
    public void handleEvent(ActivityStartEvent event) {
        if(event.getPersonId().toString().contains("EV_")){
             log.info("Persona {} inizia attività {} al link {} al tempo {}", 
                event.getPersonId(), 
                event.getActType(),
                event.getLinkId(), 
                event.getTime());
        }
    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
       if(event.getPersonId().toString().contains("EV_")){
             log.info("Persona {} finisce attività {} al link {} al tempo {}", 
                event.getPersonId(), 
                event.getActType(),
                event.getLinkId(), 
                event.getTime());
        }
    }

    @Override
    public void reset(int iteration) {}
}
