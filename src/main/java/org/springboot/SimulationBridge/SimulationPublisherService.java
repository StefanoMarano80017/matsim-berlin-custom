package org.springboot.SimulationBridge;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

import org.matsim.ServerEvSetup.SimulationInterface.SimulationBridgeInterface;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Service;

@Service
public class SimulationPublisherService {

    /* ============================================================
     * Dependencies
     * ============================================================ */
    private final SimulationBridge simulationBridge;
    private final ThreadPoolTaskScheduler taskScheduler;

    /* ============================================================
     * Scheduler configuration
     * ============================================================ */
    private ScheduledFuture<?> scheduledTask;
    private long rateMs = 5000;

    /* ============================================================
     * Simulation configuration
     * ============================================================ */
    private SimulationBridgeInterface simulationBridgeInterface;

    /**
     * true  -> snapshot incrementali (delta)
     * false -> sempre full snapshot
     */
    private boolean dirty = true;

    /* ============================================================
     * Simulation state
     * ============================================================ */
    private boolean firstSnapshotInternal = true;

    private double simTimeSeconds = 0.0;
    private static final double SIM_STEP_SECONDS = 900.0;      // 15 min
    private static final double SIM_END_SECONDS  = 24 * 3600;  // 24h

    /* ============================================================
     * Constructor
     * ============================================================ */
    public SimulationPublisherService(
            SimulationBridge simulationBridge,
            ThreadPoolTaskScheduler taskScheduler
    ) {
        this.simulationBridge = simulationBridge;
        this.taskScheduler = taskScheduler;
    }

    /* ============================================================
     * Public API
     * ============================================================ */

    /**
     * Entry point principale per avviare il publisher
     */
    public synchronized void startPublisher(
            SimulationBridgeInterface simulationBridgeInterface,
            boolean dirty,
            long rateMs
    ) {
        configureSimulation(simulationBridgeInterface, dirty);
        configureRate(rateMs);
        startScheduler();
    }

    public synchronized void stopPublisher() {
        stopScheduler();
    }

    public synchronized void updateRate(long newRateMs) {
        configureRate(newRateMs);
        restartScheduler();
    }

    /* ============================================================
     * Configuration
     * ============================================================ */

    private void configureSimulation(
            SimulationBridgeInterface simulationBridgeInterface,
            boolean dirty
    ) {
        this.simulationBridgeInterface = simulationBridgeInterface;
        this.dirty = dirty;
        resetSimulationState();
    }

    private void configureRate(long rateMs) {
        this.rateMs = rateMs;
    }

    /* ============================================================
     * Scheduler lifecycle
     * ============================================================ */

    private synchronized void startScheduler() {
        stopScheduler();

        PeriodicTrigger trigger = new PeriodicTrigger(Duration.ofMillis(rateMs));
        trigger.setFixedRate(true);

        scheduledTask = taskScheduler.schedule(
                this::publishSimulationUpdate,
                trigger
        );
    }

    private synchronized void stopScheduler() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTask = null;
        }
    }

    private void restartScheduler() {
        startScheduler();
    }

    /* ============================================================
     * Publishing logic
     * ============================================================ */

    private void publishSimulationUpdate() {
        if (simulationBridgeInterface == null) {
            return;
        }

        boolean firstSnapshotToSend = isFirstSnapshot();

        boolean started = simulationBridge.publishSimulationSnapshot(
                simulationBridgeInterface,
                simTimeSeconds,
                firstSnapshotToSend
        );

        if (started) {
            advanceSimulationTime();
        }

        updateSnapshotState();
    }


    /* ============================================================
    * Messaging API
    * ============================================================ */

    /**
     * Invia un messaggio generico alla simulazione tramite il bridge.
     * Utile per indicare eventi come inizio/fine simulazione.
     */
    public void sendSimulationMessage(String message) {
        if (simulationBridgeInterface == null || message == null || message.isBlank()) {
            return; // ignora se non inizializzato o messaggio vuoto
        }

        simulationBridge.publishSimulationMessage(message);
    }
    /**
     * Metodi helper per messaggi comuni
     */
    public void publishSimulationStart() {
        sendSimulationMessage("SIMULATION_START");
    }

    public void publishSimulationEnd() {
        sendSimulationMessage("SIMULATION_END");
    }

    /* ============================================================
     * Simulation helpers
     * ============================================================ */

    private boolean isFirstSnapshot() {
        return dirty ? firstSnapshotInternal : true;
    }

    private void advanceSimulationTime() {
        simTimeSeconds += SIM_STEP_SECONDS;

        if (simTimeSeconds >= SIM_END_SECONDS) {
            resetSimulationState();
        }
    }

    private void updateSnapshotState() {
        if (dirty) {
            firstSnapshotInternal = false;
        }
    }

    private void resetSimulationState() {
        simTimeSeconds = 0.0;
        firstSnapshotInternal = true;
    }
}
