package org.matsim.ServerEvSetup.SimulationInterface;

import java.util.List;
import java.util.stream.Collectors;

import org.matsim.CustomEvModule.ChargingHub.HubManager;
import org.matsim.CustomEvModule.EVfleet.EvFleetManager;
import org.matsim.CustomEvModule.model.EvModel;
import org.springboot.DTO.WebSocketDTO.payload.HubStatusPayload;
import org.springboot.DTO.WebSocketDTO.payload.TimeStepPayload;
import org.springboot.DTO.WebSocketDTO.payload.VehicleStatus;
import org.springframework.util.CollectionUtils;

public class SimulationBridgeInterface{
    
    private final HubManager     hubManager;
    private final EvFleetManager evFleetManager;

    public SimulationBridgeInterface(
        HubManager     hubManager, 
        EvFleetManager evFleetManager
    ){
        this.hubManager = hubManager;
        this.evFleetManager = evFleetManager;
    }

    public TimeStepPayload GetTimeStepStatus(boolean fullSnapshot){
        // --- VEICOLI ---
        List<VehicleStatus> vehicleStatuses = evFleetManager.getFleet().values().stream()
                .filter(v -> fullSnapshot || v.isDirty())
                .map(this::mapEvModelToVehicleStatus)
                .collect(Collectors.toList());

        // --- HUB ---
        List<HubStatusPayload> hubStatuses = hubManager.getHubOccupancyMap().keySet().stream()
                .filter(hubId -> fullSnapshot || hubManager.isDirty(hubId))
                .map(hubId -> new HubStatusPayload(
                    hubId,
                    hubManager.getHubEnergy(hubId),
                    hubManager.getHubOccupancy(hubId)
                ))
                .collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(vehicleStatuses) || !CollectionUtils.isEmpty(hubStatuses)) {
            return new TimeStepPayload(null, vehicleStatuses, hubStatuses);
        }

        return null;
    }

    // --- MAPPING VEICOLI ---
    private VehicleStatus mapEvModelToVehicleStatus(EvModel v) {
        return new VehicleStatus(
                v.getVehicleId().toString(),
                v.getCurrentSoc(),
                v.getDistanceTraveledKm(),
                v.getCurrentEnergyJoules(),
                v.getState() != null ? v.getState().toString() : "NULL"
        );
    }


    public void resetDirty(){
        evFleetManager.getFleet().values().forEach(EvModel::resetDirty);
        hubManager.resetDirtyFlags();
    }

}