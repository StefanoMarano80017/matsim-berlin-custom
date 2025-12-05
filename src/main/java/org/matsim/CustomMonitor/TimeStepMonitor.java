package org.matsim.CustomMonitor;

import org.matsim.CustomMonitor.ChargingHub.HubManager;
import org.matsim.CustomMonitor.EVfleet.EvFleetManager;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class TimeStepMonitor implements MobsimBeforeSimStepListener, MobsimInitializedListener {

    private final EvFleetManager evFleetManager;
    private final HubManager hubManager;
    private double stepSize = 300.0; //5 minuti
    private double lastUpdate = 0.0;
    private QSim qSim;

    @Inject
    public TimeStepMonitor(
        EvFleetManager evFleetManager, 
        HubManager hubManager, 
        @Named("timeStepMonitorStep") double stepSize) 
    {
        this.evFleetManager = evFleetManager;
        this.hubManager = hubManager;
        this.stepSize = stepSize;
    }

    @Override
    public void notifyMobsimInitialized(@SuppressWarnings("rawtypes") MobsimInitializedEvent event) {
        // Questo metodo verrà chiamato quando QSim è pronto
        if (event.getQueueSimulation() instanceof QSim) {
            this.qSim = (QSim) event.getQueueSimulation();
            System.out.println("[TimeStepMonitor] QSim associato al monitor.");
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
                System.out.println("[TimeStepMonitor] Stato veicoli aggiornato.");
            }catch(Exception e) {
                System.out.println("[TimeStepMonitor] Errore durante l'aggiornamento dello stato veicoli: " + e.getMessage());
            }
            
            // Leggo stato hub 
            hubManager.getHubOccupancyMap();       // leggi stato hub
            hubManager.getHubEnergyMap();          // leggi energia hub
            System.out.println("[TimeStepMonitor] Stato hub letto.");
            System.out.println("[TimeStepMonitor] Hub occupazione: " + hubManager.getHubOccupancyMap().toString());
            System.out.println("[TimeStepMonitor] Hub energia: " + hubManager.getHubEnergyMap().toString());

            // TODO: chiamata agente RL
            // rlAgent.updatePolicy(evFleetManager, hubManager);
        }
    }

    private ElectricFleet getElectricFleetFromQSim() {
        try {
            return qSim.getChildInjector().getInstance(ElectricFleet.class);
        } catch (Exception e) {
            return null;
        }
    }
}