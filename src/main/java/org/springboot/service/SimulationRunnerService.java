package org.springboot.service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

import org.matsim.CustomEvModule.EVfleet.EvModel;
import org.matsim.ServerEvSetup.ConfigRun.ConfigRun;
import org.matsim.ServerEvSetup.SimulationInterface.SimulationBridgeInterface;
import org.matsim.run.OpenBerlinScenario;
import org.springboot.service.GenerationService.DTO.HubSpecDto;
import org.springboot.service.SimulationState.SimulationState;
import org.springboot.service.SimulationState.SimulationStateListener;
import org.springboot.service.SimulationState.SimulationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Servizio responsabile di avviare e monitorare la simulazione.
 * Tutto lo stato della simulazione è confinato in SimulationStatus.
 */
@Service
public class SimulationRunnerService {

    private static final Logger log = LoggerFactory.getLogger(SimulationRunnerService.class);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<?> currentFuture = null;

    private final SimulationStatus status = new SimulationStatus();

    /* =========================================================
       Public API
       ========================================================= */
    
    public boolean isRunning() {
        return status.getState() == SimulationState.RUNNING;
    }

    public SimulationState getCurrentState() {
        return status.getState();
    }

    public Exception getLastException() {
        return status.getLastException();
    }

    public void setSimulationStateListener(SimulationStateListener listener) {
        status.setListener(listener);
    }

    /**
     * Esegue una simulazione in background.
     */
    public synchronized String runAsync(List<EvModel> evModels, List<HubSpecDto> hubSpecs, ConfigRun config) {
        if (status.getState() == SimulationState.RUNNING) {
            return "Simulazione già in esecuzione.";
        }

        // reset stato precedente
        status.reset();
        currentFuture = executor.submit(() -> createSimulationTask(evModels, hubSpecs, config));

        return "Simulazione avviata.";
    }

    /**
     * Arresta la simulazione in corso.
     */
    public synchronized String stop() {
        if (status.getState() != SimulationState.RUNNING) {
            return "Nessuna simulazione attiva.";
        }

        try {
            if (currentFuture != null) currentFuture.cancel(true);

            // Aggiorna lo stato e notifica il listener
            status.update(SimulationState.STOPPED, null, null, true);

            return "Richiesta di interruzione inviata.";
        } catch (Exception e) {
            status.update(SimulationState.ERROR, null, e, true);
            log.error("Errore durante l'arresto della simulazione", e);
            return "Errore durante l'arresto: " + e.getMessage();
        } 
    }

    /**
     * Accesso thread-safe al bridge con un Consumer.
     */
    public void withCurrentSimulationBridge(Consumer<SimulationBridgeInterface> action) {
        SimulationBridgeInterface bridge;
        synchronized (this) {
            if (status.getState() != SimulationState.RUNNING || status.getBridge() == null) return;
            bridge = status.getBridge();
        }
        action.accept(bridge);
    }

    /**
     * Accesso thread-safe al bridge con un Function che restituisce un valore.
     */
    public <T> T mapCurrentSimulationBridge(Function<SimulationBridgeInterface, T> mapper) {
        SimulationBridgeInterface bridge;
        synchronized (this) {
            if (status.getState() != SimulationState.RUNNING || status.getBridge() == null) return null;
            bridge = status.getBridge();
        }
        return mapper.apply(bridge);
    }

    /* =========================================================
       Private helper methods
       ========================================================= */
    private Runnable createSimulationTask(List<EvModel> evModels, List<HubSpecDto> hubSpecs, ConfigRun config) {
        return () -> {
            try {
                runSimulation(evModels, hubSpecs, config);
            } catch (InterruptedException e) {
                handleSimulationInterruption(e);
                Thread.currentThread().interrupt();
            } catch (Throwable t) {
                handleSimulationError(t);
            }
        };
    }

    private void runSimulation(List<EvModel> evModels, List<HubSpecDto> hubSpecs, ConfigRun config) throws InterruptedException, Exception {
        OpenBerlinScenario scenarioApp = new OpenBerlinScenario().withConfigRun(config);
        SimulationBridgeInterface bridge = scenarioApp.SetupSimulationWithServerModels(evModels, hubSpecs);

        // Aggiorna stato RUNNING e notifica listener
        status.update(SimulationState.RUNNING, bridge, null, true);
        log.info("Simulazione avviata con successo");

        scenarioApp.run();

        // Al termine, aggiorna stato COMPLETED
        status.update(SimulationState.COMPLETED, null, null, true);
        log.info("Simulazione completata con successo");
    }

    private void handleSimulationInterruption(InterruptedException e) {
        status.update(SimulationState.STOPPED, null, e, true);
        log.warn("Simulazione interrotta", e);
        Thread.currentThread().interrupt();
    }

    private void handleSimulationError(Throwable t) {
        status.update(SimulationState.ERROR, null, t instanceof Exception ? (Exception) t : new Exception(t), true);
        log.error("Errore durante l'esecuzione della simulazione", t);
    }
    
}
