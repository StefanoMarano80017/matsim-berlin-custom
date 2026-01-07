package org.matsim.ServerEvSetup.SimulationInterface;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.CustomEvModule.Hub.HubManager;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.CustomEvModule.EVfleet.EvFleetManager;
import org.matsim.CustomEvModule.EVfleet.EvModel;
import org.springboot.DTO.WebSocketDTO.payload.ChargerStatus;
import org.springboot.DTO.WebSocketDTO.payload.HubStatusPayload;
import org.springboot.DTO.WebSocketDTO.payload.TimeStepPayload;
import org.springboot.DTO.WebSocketDTO.payload.VehicleStatus;
import org.springframework.util.CollectionUtils;

public class SimulationBridgeInterface implements IterationStartsListener {

    private final HubManager     hubManager;
    private final EvFleetManager evFleetManager;

    private volatile boolean simulationStarted = false;

    public SimulationBridgeInterface(HubManager hubManager, EvFleetManager evFleetManager) {
        this.hubManager = hubManager;
        this.evFleetManager = evFleetManager;
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        simulationStarted = true; // la simulazione è iniziata
    }

    // Metodo per sapere se la simulazione è partita
    public boolean isSimulationStarted() {
        return simulationStarted;
    }

    public TimeStepPayload GetTimeStepStatus(boolean fullSnapshot) {
        // Non restituire nulla se la simulazione non è partita
        if (!simulationStarted) return null;

        // --- VEICOLI ---
        List<VehicleStatus> vehicleStatuses = evFleetManager.getFleet().values().stream()
                .filter(v -> fullSnapshot || v.isDirty())
                .map(this::mapEvModelToVehicleStatus)
                .collect(Collectors.toList());

        // --- HUB ---
        List<HubStatusPayload> hubStatuses = hubManager.getAllHubs().stream()
                .filter(hub -> fullSnapshot || hub.isDirty())
                .map(hub -> {
                    Map<String, ChargerStatus> chargerStates = new HashMap<>();
                    
                    hub.getChargers().forEach(chId -> {
                        var evId = hub.getEvOccupyingCharger(chId);
                        boolean occupied = evId != null;

                        chargerStates.put(
                            chId.toString(),
                            new ChargerStatus(
                                chId.toString(),
                                occupied,
                                hub.getChargerEnergy(chId),
                                occupied ? evId.toString() : null
                            )
                        );
                    });
                    
                    return new HubStatusPayload(
                            hub.getId(),
                            hub.getTotalEnergy(),
                            hub.getOccupancy(),
                            chargerStates
                    );
                })
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

    public void resetDirty() {
        evFleetManager.getFleet().values().forEach(EvModel::resetDirty);
        hubManager.resetDirtyFlags();
    }


}
