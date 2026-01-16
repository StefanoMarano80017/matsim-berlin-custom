package org.matsim.ServerEvSetup.SimulationInterface;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.CustomEvModule.Hub.ChargingHub;
import org.matsim.CustomEvModule.Hub.HubManager;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.vehicles.Vehicle;
import org.matsim.CustomEvModule.EVfleet.EvFleetManager;
import org.matsim.CustomEvModule.EVfleet.EvModel;
import org.springboot.DTO.WebSocketDTO.payload.ChargerStatus;
import org.springboot.DTO.WebSocketDTO.payload.HubStatusPayload;
import org.springboot.DTO.WebSocketDTO.payload.TimeStepPayload;
import org.springboot.DTO.WebSocketDTO.payload.VehicleStatus;
import org.springboot.service.GenerationService.DTO.HubSpecDto;
import org.springframework.util.CollectionUtils;

public class SimulationBridgeInterface implements IterationStartsListener {

    private final HubManager     hubManager;
    private final EvFleetManager evFleetManager;

    private volatile boolean simulationStarted = false;
    
    /**
     * Timestep REALE della simulazione MATSim.
     * Aggiornato dai Monitoring quando ricevono gli eventi da QSim.
     */
    private volatile double currentSimTime = 0.0;

    public SimulationBridgeInterface(HubManager hubManager, EvFleetManager evFleetManager) {
        this.hubManager      = hubManager;
        this.evFleetManager  = evFleetManager;
    }

    /**
     * Riceve i modelli EV pre-generati dal server e li registra nel manager.
     * Questo metodo è chiamato prima della simulazione.
     * 
     * @param evModels Lista di EvModel già generati dal server
     */
    public void initializeEvModels(List<EvModel> evModels) {
        if (evModels == null || evModels.isEmpty()) {
            throw new IllegalArgumentException("evModels cannot be null or empty");
        }
        evFleetManager.registerEvModels(evModels);
    }

    /**
     * Riceve gli hub di ricarica pre-generati dal server e li registra nel manager.
     * Questo metodo è chiamato prima della simulazione.
     * 
     * Nota: Accetta i modelli di dominio puri (HubSpecDto) generati dal server,
     * NON le specifiche MATSim. La registrazione nell'infrastruttura viene fatta
     * dal manager.
     * 
     * @param hubSpecs Lista di specifiche hub (modelli di dominio server)
     */
    public void initializeChargingHubsFromServerModels(List<HubSpecDto> hubSpecs) {
        if (hubSpecs == null || hubSpecs.isEmpty()) {
            throw new IllegalArgumentException("hubSpecs cannot be null or empty");
        }
        hubManager.registerChargingHubsFromSpecs(hubSpecs);
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        simulationStarted = true; // la simulazione è iniziata
    }

    // Metodo per sapere se la simulazione è partita
    public boolean isSimulationStarted() {
        return simulationStarted;
    }

    /**
     * Imposta il timestep REALE della simulazione.
     * Chiamato dai Monitoring quando ricevono gli eventi di MATSim.
     * 
     * @param simTime Il tempo di simulazione reale in secondi
     */
    public void setCurrentSimTime(double simTime) {
        this.currentSimTime = simTime;
    }

    /**
     * Ritorna il timestep REALE della simulazione.
     * 
     * @return Il tempo di simulazione reale in secondi
     */
    public double getCurrentSimTime() {
        return currentSimTime;
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
                        String evId = hub.getEvOccupyingCharger(chId);
                        boolean occupied = evId != null;

                        chargerStates.put(
                            chId.toString(),
                            new ChargerStatus(
                                chId.toString(),
                                occupied,
                                hub.getChargerEnergy(chId),
                                occupied ? evId : null
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

    /* ============================================================
     * Centralized operations on managers (for Monitoring classes)
     * ============================================================ */

    /**
     * Update SoC of all EV vehicles.
     * Called by TimeStepSocMonitor instead of accessing evFleetManager directly.
     * 
     * @param electricFleet The electric fleet to use for updates
     */
    public void updateEvFleetSoC(org.matsim.contrib.ev.fleet.ElectricFleet electricFleet) {
        if (electricFleet != null) {
            evFleetManager.updateSoc(electricFleet);
        }
    }

    /**
     * Update the state of a single EV vehicle.
     * Called by VehicleStatusMonitor instead of accessing evFleetManager directly.
     * 
     * @param vehicleId The vehicle ID
     * @param state The new state (IDLE, MOVING, PARKED, CHARGING, etc.)
     */
    public void updateEvState(Id<Vehicle> vehicleId, EvModel.State state) {
        EvModel vehModel = evFleetManager.getVehicle(vehicleId);
        if (vehModel != null) {
            vehModel.setState(state);
        }
    }

    /**
     * Handle charging start event for a vehicle at a charging hub.
     * 
     * @param chargerId The charger ID where charging starts
     * @param vehicleId The vehicle ID that starts charging
     * @param simTime The simulation time
     */
    public void handleChargingStart(
            Id<Charger> chargerId,
            Id<Vehicle> vehicleId,
            double simTime
    ) {
        try {
            String hubId = hubManager.getHubIdForCharger(chargerId);
            ChargingHub hub = hubManager.getHub(hubId);
            if (hub != null) {
                hub.incrementOccupancy(chargerId, vehicleId.toString(), 0.0); // energia a start=0
            }
        } catch (Exception e) {
            // Hub or charger not found - ignore
        }
    }

    /**
     * Handle charging end event for a vehicle at a charging hub.
     * Called by HubChargingMonitor instead of accessing hubManager directly.
     * 
     * @param chargerId The charger ID where charging ends
     * @param energy The energy charged
     */
    public void handleChargingEnd(
            Id<Charger> chargerId,
            double energy
    ) {
        try {
            String hubId = hubManager.getHubIdForCharger(chargerId);
            ChargingHub hub = hubManager.getHub(hubId);
            if (hub != null) {
                hub.decrementOccupancy(chargerId, energy);
            }
        } catch (Exception e) {
            // Hub or charger not found - ignore
        }
    }

}

