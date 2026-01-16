package org.matsim.CustomEvModule.Monitoring;

import org.matsim.CustomEvModule.EVfleet.EvModel;
import org.matsim.ServerEvSetup.SimulationInterface.SimulationBridgeInterface;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.contrib.ev.charging.ChargingEndEvent;
import org.matsim.contrib.ev.charging.ChargingEndEventHandler;
import org.matsim.contrib.ev.charging.ChargingStartEvent;
import org.matsim.contrib.ev.charging.ChargingStartEventHandler;
import org.matsim.vehicles.Vehicle;

public class VehicleStatusMonitor implements ChargingStartEventHandler, 
                                            ChargingEndEventHandler, 
                                            VehicleEntersTrafficEventHandler,
                                            ActivityStartEventHandler, 
                                            ActivityEndEventHandler{

    private final SimulationBridgeInterface simulationBridgeInterface;

    public VehicleStatusMonitor(SimulationBridgeInterface simulationBridgeInterface) {
        this.simulationBridgeInterface = simulationBridgeInterface;
    }


    @Override
    public void handleEvent(ChargingEndEvent event) {
        simulationBridgeInterface.updateEvState(event.getVehicleId(), EvModel.State.IDLE);
    }

    @Override
    public void handleEvent(ChargingStartEvent event) {
        simulationBridgeInterface.updateEvState(event.getVehicleId(), EvModel.State.CHARGING);
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        simulationBridgeInterface.updateEvState(event.getVehicleId(), EvModel.State.MOVING);
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        Id<Vehicle> vehicleId = Id.createVehicleId(event.getPersonId().toString() + "_car");
        simulationBridgeInterface.updateEvState(vehicleId, EvModel.State.PARKED);
    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
        Id<Vehicle> vehicleId = Id.createVehicleId(event.getPersonId().toString() + "_car");
        simulationBridgeInterface.updateEvState(vehicleId, EvModel.State.IDLE);
    }
    
}
