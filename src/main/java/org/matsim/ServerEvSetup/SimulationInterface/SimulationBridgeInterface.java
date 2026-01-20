package org.matsim.ServerEvSetup.SimulationInterface;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.vehicles.Vehicle;
import org.matsim.CustomEvModule.EVfleet.EvFleetManager;
import org.matsim.CustomEvModule.EVfleet.EvModel;
import org.matsim.CustomEvModule.Hub.HubManager;
import org.springboot.DTO.WebSocketDTO.payload.TimeStepPayload;

public class SimulationBridgeInterface implements IterationStartsListener {

    private final SimulationLifecycleService lifecycle;
    private final TimeStepStatusService statusService;
    private final DataCommandService dataCommands;
    
    public SimulationBridgeInterface(
        EvFleetManager evFleetManager,
        HubManager hubManager
    ) {
        this.lifecycle      = new SimulationLifecycleService();
        this.statusService  = new TimeStepStatusService(hubManager, evFleetManager);
        this.dataCommands   = new DataCommandService(evFleetManager, hubManager);
    }

    /*
    *    LIFECYCLE 
    */
    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        lifecycle.setsimulationStarted(true);
    }

    // Metodo per sapere se la simulazione è partita
    public boolean isSimulationStarted() {
        return lifecycle.isSimulationStarted();
    }

    /**
     * Imposta il timestep REALE della simulazione.
     * Chiamato dai Monitoring quando ricevono gli eventi di MATSim.
     * 
     * @param simTime Il tempo di simulazione reale in secondi
     */
    public void setCurrentSimTime(double simTime) {
       lifecycle.setCurrentSimTime(simTime);
    }

    /**
     * Ritorna il timestep REALE della simulazione.
     * 
     * @return Il tempo di simulazione reale in secondi
     */
    public double getCurrentSimTime() {
        return lifecycle.getCurrentSimTime();
    }

    /* 
    *    STATUS 
    */
    public TimeStepPayload GetTimeStepStatus(boolean fullSnapshot) {
       return statusService.buildPayload(fullSnapshot);
    }

    public void resetDirty(){
        statusService.resetDirty();
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
        dataCommands.updateFleetSoc(electricFleet);
    }

    /**
     * Update the state of a single EV vehicle.
     * Called by VehicleStatusMonitor instead of accessing evFleetManager directly.
     * 
     * @param vehicleId The vehicle ID
     * @param state The new state (IDLE, MOVING, PARKED, CHARGING, etc.)
     */
    public void updateEvState(Id<Vehicle> vehicleId, EvModel.State state) {
        dataCommands.updateEvState(vehicleId, state);
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
       dataCommands.handleChargingStart(chargerId, vehicleId.toString());
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
        dataCommands.handleChargingEnd(chargerId, energy);
    }

    /**
     * Resetta l'energia in erogazione di tutte le colonnine di tutti gli hub.
     * Deve essere chiamato all'inizio di ogni timestep.
     */
    public void resetChargersCurrentEnergy() {
        dataCommands.resetCurrentEnergy();
    }

    /**
     * Aggiorna l'energia che le colonnine stanno erogando in questo timestep.
     * Legge lo stato della flotta EV dalla simulazione e calcola quanta energia
     * ogni colonnina sta erogando.
     * 
     * @param electricFleet La flotta EV da cui leggere i dati
     */
    public void updateChargersEnergyDelivering() {
        dataCommands.updateEnergyDelivering();
    }

}

