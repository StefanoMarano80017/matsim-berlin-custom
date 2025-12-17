package org.matsim.CustomMonitor.Monitoring;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.CustomMonitor.EVfleet.EvFleetManager;
import org.matsim.CustomMonitor.model.EvModel;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.springboot.DTO.WebSocketDTO.payload.VehicleStatus;
import org.springboot.SimulationBridge.SimulationEventBus;

import com.google.inject.name.Named;

public class LinkMonitor implements LinkEnterEventHandler, VehicleEntersTrafficEventHandler{
    private static final Logger log = LogManager.getLogger(QuickLinkDebugHandler.class);

    private final EvFleetManager evFleetManager;

    private final SimulationEventBus eventBus;
    private final Boolean publish_on_spring;

    public LinkMonitor(
        EvFleetManager evFleetManager,
        @Named("serverEnabled") boolean publish_on_spring
    ) {
        this.evFleetManager = evFleetManager;
        this.publish_on_spring = publish_on_spring;
        if(publish_on_spring){
            this.eventBus = SimulationEventBus.getInstance();
        } else{
            this.eventBus = null;
        }
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        EvModel vehModel = evFleetManager.getVehicle(event.getVehicleId());
        if (vehModel != null){
            log.debug("Veicolo {} entra link {} al tempo {}", event.getVehicleId(), event.getLinkId(), event.getTime());
            vehModel.setState(EvModel.State.MOVING);
            if(publish_on_spring){
                VehicleStatus payload = new VehicleStatus(
                    vehModel.getVehicleId().toString(),
                    vehModel.getCurrentSoc(),
                    vehModel.getDistanceTraveledKm(),
                    vehModel.getCurrentEnergyJoules(),
                    vehModel.getState().toString(),
                    event.getLinkId().toString(),
                    event.getTime()
                );
                //eventBus.publish(payload);
            }
        }
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {     
        EvModel vehModel = evFleetManager.getVehicle(event.getVehicleId());
        if (vehModel != null){
            log.info("Veicolo {} ha iniziato il viaggio sul link {} al tempo {}", event.getVehicleId(), event.getLinkId(), event.getTime());
            vehModel.setState(EvModel.State.MOVING);
            if(publish_on_spring){                
                VehicleStatus payload = new VehicleStatus(
                    vehModel.getVehicleId().toString(),
                    vehModel.getCurrentSoc(),
                    vehModel.getDistanceTraveledKm(),
                    vehModel.getCurrentEnergyJoules(),
                    vehModel.getState().toString(),
                    event.getLinkId().toString(),
                    event.getTime()
                );
                eventBus.publish(payload);
            }
        }
    
    }
}
