package org.matsim.CustomMonitor;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.CustomMonitor.ChargingHub.HubManager;
import org.matsim.CustomMonitor.EVfleet.EvFleetManager;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.springboot.DTO.payload.VehicleStatus;
import org.springboot.websocket.SimulationBridge;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class TimeStepMonitor implements MobsimBeforeSimStepListener, MobsimInitializedListener {

    private static final Logger log = LogManager.getLogger(EvFleetManager.class);

    private final EvFleetManager evFleetManager;
    private final HubManager hubManager;
    private double stepSize = 300.0; //5 minuti
    private double lastUpdate = 0.0;
    private QSim qSim;
    private final SimulationBridge simulationBridge;

    @Inject
    public TimeStepMonitor(
        EvFleetManager evFleetManager, 
        HubManager hubManager, 
        @Named("timeStepMonitorStep") double stepSize, 
        @Nullable SimulationBridge simulationBridge
    ) 
    {
        this.evFleetManager = evFleetManager;
        this.hubManager = hubManager;
        this.stepSize = stepSize;
        this.simulationBridge = simulationBridge;
    }

    @Override
    public void notifyMobsimInitialized(@SuppressWarnings("rawtypes") MobsimInitializedEvent event) {
        // Questo metodo verrà chiamato quando QSim è pronto
        if (event.getQueueSimulation() instanceof QSim) {
            this.qSim = (QSim) event.getQueueSimulation();
            log.info("[TimeStepMonitor] QSim associato al monitor.");
        }
    }

    @Override
    public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent event) {
        double simTime = event.getSimulationTime();
        if (qSim == null) return;
        if (simTime - lastUpdate >= stepSize) {
            lastUpdate = simTime;
            // Aggiornamento Soc Veicoli 
            try{
                ElectricFleet electricFleet = getElectricFleetFromQSim();
                evFleetManager.updateSoc(electricFleet);
                // Pubblico evento
                publishEvent(simTime);
            }catch(Exception e) {
                log.error("[TimeStepMonitor] Errore durante l'aggiornamento dello stato veicoli: " + e.getMessage());
            }
            
        }
    }

    private ElectricFleet getElectricFleetFromQSim() {
        try {
            return qSim.getChildInjector().getInstance(ElectricFleet.class);
        } catch (Exception e) {
            return null;
        }
    }

    private void publishEvent(double simTime){
        if(this.simulationBridge != null){
            List<VehicleStatus> vehicleStatuses = 
                evFleetManager.getFleet().values().stream().map(v -> new VehicleStatus(
                    v.getVehicleId().toString(),
                    v.getCurrentSoc(),
                    v.getDistanceTraveledKm(),
                    v.getCurrentEnergyJoules(),
                    v.isCharging()
                ))
                .collect(Collectors.toList());

            this.simulationBridge.publishTimeStep(
                vehicleStatuses, 
                hubManager.getHubOccupancyMap(), 
                hubManager.getHubEnergyMap(),
                simTime
            );
        }
    }
}