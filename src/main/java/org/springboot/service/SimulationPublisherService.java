package org.springboot.service;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

import org.matsim.ServerEvSetup.SimulationInterface.SimulationBridgeInterface;
import org.springboot.DTO.WebSocketDTO.payload.TimeStepPayload;
import org.springboot.SimulationBridge.SimulationDataExtractor;
import org.springboot.websocket.SimulationWebSocketPublisher;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsabilità: Orchestrazione dello scheduling e timing della simulazione.
 * 
 * Coordina:
 * 1. Il timing e la frequenza di pubblicazione (scheduling)
 * 2. L'estrazione dati (delegando a SimulationDataExtractor)
 * 3. La pubblicazione via WebSocket (delegando a SimulationWebSocketPublisher)
 * 
 * NON estrae dati direttamente, NON pubblica direttamente via WebSocket.
 * Delega queste responsabilità alle classi specializzate.
 */
@Service
public class SimulationPublisherService {

    private static final Logger logger = LoggerFactory.getLogger(SimulationPublisherService.class);

    /* ============================================================
     * Dependencies
     * ============================================================ */
    private final SimulationDataExtractor dataExtractor;
    private final SimulationWebSocketPublisher wsPublisher;
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

    /* ============================================================
     * Constructor
     * ============================================================ */
    public SimulationPublisherService(
            SimulationDataExtractor dataExtractor,
            SimulationWebSocketPublisher wsPublisher,
            ThreadPoolTaskScheduler taskScheduler
    ) {
        this.dataExtractor = dataExtractor;
        this.wsPublisher = wsPublisher;
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

    /**
     * Invia un messaggio generico alla simulazione tramite WebSocket.
     * Utile per indicare eventi come inizio/fine simulazione.
     */
    public void sendSimulationMessage(String message) {
        wsPublisher.publishMessage(message);
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
     * Configuration
     * ============================================================ */
    
    private void configureSimulation(
            SimulationBridgeInterface simulationBridgeInterface,
            boolean dirty
    ) {
        this.simulationBridgeInterface = simulationBridgeInterface;
        this.dirty = dirty;
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
     * Publishing orchestration
     * ============================================================ */

    /**
     * Orchestrazione: estrai dati, pubblica, gestisci timing.
     */
    private void publishSimulationUpdate() {
        if (simulationBridgeInterface == null) {
            return;
        }

        try {
            // 1. Determina se è il primo snapshot
            boolean firstSnapshotToSend = isFirstSnapshot();
            
            // 2. Estrai dati dalla simulazione
            TimeStepPayload payload = dataExtractor.extractTimeStepSnapshot(
                simulationBridgeInterface,
                firstSnapshotToSend
            );
            
            if (payload == null) {
                return; // Nessun dato da pubblicare
            }

            // 3. Imposta il timestamp (legge il timestep REALE dal bridge, non virtuale)
            dataExtractor.setTimestamp(payload, simulationBridgeInterface);
            
            // 4. Pubblica via WebSocket
            boolean published = wsPublisher.publishTimeStepSnapshot(payload);
            
            if (published) {
                // 5. Resetta i flag dirty se è stato uno snapshot delta
                if (!firstSnapshotToSend) {
                    dataExtractor.resetDirtyFlags(simulationBridgeInterface);
                }
                
                // Note: Il timestep reale viene impostato dalle classi di Monitoring
                // attraverso simulationBridgeInterface.setCurrentSimTime(simTime)
            }
            
            // 7. Aggiorna lo stato dello snapshot
            updateSnapshotState();
            
        } catch (Exception e) {
            logger.error("Errore durante publishSimulationUpdate", e);
        }
    }

    /* ============================================================
     * Snapshot state management
     * ============================================================ */

    private boolean isFirstSnapshot() {
        return dirty ? firstSnapshotInternal : true;
    }

    private void updateSnapshotState() {
        if (dirty) {
            firstSnapshotInternal = false;
        }
    }
}

