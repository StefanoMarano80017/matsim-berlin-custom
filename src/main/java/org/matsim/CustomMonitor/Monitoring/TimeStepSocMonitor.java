package org.matsim.CustomMonitor.Monitoring;

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

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class TimeStepSocMonitor implements MobsimBeforeSimStepListener, MobsimInitializedListener {

    private static final Logger log = LogManager.getLogger(TimeStepSocMonitor.class);

    private final EvFleetManager evFleetManager;
    private final HubManager hubManager;
    private final double stepSize;
    private double lastUpdate = 0.0;
    private QSim qSim;

    @Inject
    public TimeStepSocMonitor(
            EvFleetManager evFleetManager,
            HubManager hubManager,
            @Named("timeStepMonitorStep") double stepSize
    ) {
        this.evFleetManager = evFleetManager;
        this.hubManager     = hubManager;
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
                    //publishTimeStep(simTime);
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

    /*
     
    private void publishTimeStep(double simTime) {
        List<VehicleStatus> vehicleStatuses =
            evFleetManager.getFleet().values().stream().map(v -> new VehicleStatus(
                v.getVehicleId().toString(),
                v.getCurrentSoc(),
                v.getDistanceTraveledKm(),
                v.getCurrentEnergyJoules(),
                v.getState().toString()
            ))
            .collect(Collectors.toList());

        Map<String, Integer> hubOccupancyMap = hubManager.getHubOccupancyMap();
        Map<String, Double> hubEnergyMap     = hubManager.getHubEnergyMap();
        TimeStepPayload payload              = new TimeStepPayload(simTime, vehicleStatuses, buildHubStatusPayload(hubOccupancyMap, hubEnergyMap));

        publishEvent(payload);
    }
        */
}
