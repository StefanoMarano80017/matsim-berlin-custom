package org.matsim.CustomMonitor;

import org.matsim.CustomMonitor.ChargingHub.HubManager;
import org.matsim.CustomMonitor.EVfleet.EvFleetManager;
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
    public TimeStepMonitor(EvFleetManager evFleetManager, HubManager hubManager, @Named("timeStepMonitorStep") double stepSize) {
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
            evFleetManager.updateSocFromQSim(qSim);
            System.out.println("[TimeStepMonitor] Stato veicoli aggiornato.");

            // Leggo stato hub 
            hubManager.getHubOccupancyMap();       // leggi stato hub
            hubManager.getHubEnergyMap();          // leggi energia hub
            System.out.println("[TimeStepMonitor] Stato hub letto.");

            // TODO: chiamata agente RL
            // rlAgent.updatePolicy(evFleetManager, hubManager);

            System.out.printf("[%.0f] Monitor aggiornato: SOC medio=%.2f%n", simTime, evFleetManager.calculateAverageSoc());
        }
    }

}