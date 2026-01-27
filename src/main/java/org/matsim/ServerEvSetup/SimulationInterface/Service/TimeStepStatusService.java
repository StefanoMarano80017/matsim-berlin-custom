package org.matsim.ServerEvSetup.SimulationInterface.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.CustomEvModule.EVfleet.EvFleetManager;
import org.matsim.CustomEvModule.EVfleet.EvModel;
import org.matsim.CustomEvModule.Hub.ChargerUnit;
import org.matsim.CustomEvModule.Hub.ChargingHub;
import org.matsim.CustomEvModule.Hub.HubManager;
import org.springboot.DTO.WebSocketDTO.payload.ChargerStatus;
import org.springboot.DTO.WebSocketDTO.payload.HubStatusPayload;
import org.springboot.DTO.WebSocketDTO.payload.TimeStepPayload;
import org.springboot.DTO.WebSocketDTO.payload.VehicleStatus;

public class TimeStepStatusService {

    private final HubManager hubManager;
    private final EvFleetManager evFleetManager;

    public TimeStepStatusService(
        HubManager hubManager,
        EvFleetManager evFleetManager
    ) {
        this.hubManager = hubManager;
        this.evFleetManager = evFleetManager;
    }

    public TimeStepPayload buildPayload(boolean fullSnapshot) {

        List<VehicleStatus> vehicles = evFleetManager.getFleet()
                .values()
                .stream()
                .filter(v -> fullSnapshot || v.isDirty())
                .map(this::mapVehicle)
                .toList();

        List<HubStatusPayload> hubs = hubManager.getAllHubs()
                .stream()
                .filter(h -> fullSnapshot || h.isDirty())
                .map(this::mapHub)
                .toList();

        if (vehicles.isEmpty() && hubs.isEmpty()) {
            return null;
        }

        return new TimeStepPayload(null, vehicles, hubs);
    }

    /*  ================================================
    *   MAPPERS
        ================================================ */

    private VehicleStatus mapVehicle(EvModel v) {
        return new VehicleStatus(
                v.getVehicleId().toString(),
                v.getCurrentSoc(),
                v.getDistanceTraveledKm(),
                v.getCurrentEnergyJoules(),
                v.getState() != null ? v.getState().toString() : "NULL"
        );
    }

    private ChargerStatus mapCharger(ChargerUnit  cu){
        return new ChargerStatus(
                    cu.getChargerId().toString(),
                    cu.getOccupyingEvId() != null,
                    cu.isActive(),
                    cu.getCumulativeEnergyDelivered(),
                    cu.getCurrentEnergyDelivering(),
                    cu.getOccupyingEvId()
                );
    }

    private HubStatusPayload mapHub(ChargingHub hub) {
        Map<String, ChargerStatus> chargers = new HashMap<>();

        hub.getChargerUnits().forEach(cu -> {
            chargers.put(
                cu.getChargerId().toString(),
                mapCharger(cu)
            );
        });

        return new HubStatusPayload(
                hub.getId(),
                hub.getTotalEnergy(),
                hub.getOccupancy(),
                chargers
        );
    }

    public void resetDirty() {
        evFleetManager.getFleet().values().forEach(EvModel::resetDirty);
        hubManager.resetDirtyFlags();
    }
}
