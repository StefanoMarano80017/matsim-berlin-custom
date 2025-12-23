package org.matsim.CustomEvModule.EVfleet.factory;

import org.matsim.CustomEvModule.EVfleet.EvModel;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.fleet.*;
import org.matsim.vehicles.*;

import java.util.Set;

public class EvVehicleFactory {

    public void createMatsimVehicle(
        EvModel model, 
        Vehicles vehicles,  
        Set<String> chargerTypes
    ) {
        Id<VehicleType> typeId = Id.create(model.getModel(), VehicleType.class);
        VehicleType type = vehicles.getVehicleTypes().get(typeId);
        if (type == null){
            type = createVehicleType(typeId, model, chargerTypes);
            vehicles.addVehicleType(type);
        }

        Id<Vehicle> vId = Id.create(model.getVehicleId().toString() + "_car", Vehicle.class);
        Vehicle vehicle = VehicleUtils.createVehicle(vId, type);

        vehicle.getAttributes().putAttribute(ElectricFleetUtils.INITIAL_SOC, model.getCurrentSoc());
        vehicles.addVehicle(vehicle);
    }

    private VehicleType createVehicleType(
        Id<VehicleType> typeId,
        EvModel model, 
        Set<String> chargerTypes //Set.of("default", "a", "b")
    ) {
        VehicleType type = VehicleUtils.createVehicleType(typeId);
        type.setMaximumVelocity(model.getTopSpeedKmh() * 3.6);
        EngineInformation engineInfo = type.getEngineInformation();
        VehicleUtils.setEnergyCapacity(engineInfo, model.getNominalCapacityKwh());
        VehicleUtils.setHbefaTechnology(engineInfo, ElectricFleetUtils.EV_ENGINE_HBEFA_TECHNOLOGY);
        engineInfo.getAttributes().putAttribute(ElectricFleetUtils.CHARGER_TYPES, chargerTypes);
        return type;
    }

}
