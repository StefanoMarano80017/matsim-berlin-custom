package org.matsim.CustomEvModule.Monitoring;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.CustomEvModule.EVfleet.EvFleetManager;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class TimeStepSocMonitor implements MobsimBeforeSimStepListener, MobsimInitializedListener {

    private static final Logger log = LogManager.getLogger(TimeStepSocMonitor.class);

    private final EvFleetManager evFleetManager;
    private final double stepSize;
    private double lastUpdate = 0.0;
    private QSim qSim;

    @Inject
    public TimeStepSocMonitor(
            EvFleetManager evFleetManager,
            @Named("timeStepMonitorStep") double stepSize
    ) {
        this.evFleetManager = evFleetManager;
        this.stepSize       = stepSize;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void notifyMobsimInitialized(MobsimInitializedEvent event) {
        if (event.getQueueSimulation() instanceof QSim) {
            this.qSim = (QSim) event.getQueueSimulation();
            log.info("[TimeStepMonitor] QSim associato al monitor.");
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent event) {
        double simTime = event.getSimulationTime();
        if (qSim == null) return;
        if (simTime - lastUpdate >= stepSize) {
            lastUpdate = simTime;
            try {
                ElectricFleet electricFleet = getElectricFleetFromQSim();
                if (electricFleet != null) {
                    evFleetManager.updateSoc(electricFleet);
                }
            } catch (Exception e) {
                log.error("[TimeStepMonitor] Errore aggiornamento stato veicoli: {}", e.getMessage());
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
}
