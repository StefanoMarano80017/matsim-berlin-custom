package org.matsim.CustomEvModule.Monitoring;

import org.matsim.CustomEvModule.EVfleet.EvModel;
import org.matsim.CustomEvModule.Utils.CoordinateConverter;
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
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;

public class VehicleStatusMonitor implements ChargingStartEventHandler, 
                                            ChargingEndEventHandler, 
                                            VehicleEntersTrafficEventHandler,
                                            LinkEnterEventHandler,
                                            LinkLeaveEventHandler,
                                            ActivityStartEventHandler, 
                                            ActivityEndEventHandler{

    private final SimulationBridgeInterface simulationBridgeInterface;
    private final Network network;

    public VehicleStatusMonitor(SimulationBridgeInterface simulationBridgeInterface, Network network) {
        this.simulationBridgeInterface = simulationBridgeInterface;
        this.network = network;
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
        Link link = network.getLinks().get(event.getLinkId());
        Coord from = link.getToNode().getCoord();
        SetCoordVehicle(
            event.getVehicleId(),
            from.getX(),
            from.getY()
        );
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        Id<Vehicle> vehicleId = Id.createVehicleId(event.getPersonId().toString() + "_car");
        simulationBridgeInterface.updateEvState(vehicleId, EvModel.State.PARKED);
        Link link = network.getLinks().get(event.getLinkId());
        Coord from = link.getToNode().getCoord();
        SetCoordVehicle(
            vehicleId,
            from.getX(),
            from.getY()
        );
    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
        Id<Vehicle> vehicleId = Id.createVehicleId(event.getPersonId().toString() + "_car");
        simulationBridgeInterface.updateEvState(vehicleId, EvModel.State.IDLE);
        Link link = network.getLinks().get(event.getLinkId());
        Coord from = link.getToNode().getCoord();
        SetCoordVehicle(
            vehicleId,
            from.getX(),
            from.getY()
        );
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        Link link = network.getLinks().get(event.getLinkId());
        Coord from = link.getFromNode().getCoord();
        SetCoordVehicle(
                event.getVehicleId(),
                from.getX(),
                from.getY()
        );
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        Link link = network.getLinks().get(event.getLinkId());
        Coord from = link.getToNode().getCoord();
        SetCoordVehicle(
                event.getVehicleId(),
                from.getX(),
                from.getY()
        );
    }   

    private void SetCoordVehicle(Id<Vehicle> vehicleId, double x, double y) {
        double[] latLon = CoordinateConverter.toLatLon(x, y);
        simulationBridgeInterface.updateEvPosition(vehicleId, latLon[0], latLon[1]);
    }


}
