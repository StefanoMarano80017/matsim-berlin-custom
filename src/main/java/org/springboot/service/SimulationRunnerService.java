package org.springboot.service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.matsim.CustomEvModule.EVfleet.EvModel;
import org.matsim.ServerEvSetup.ConfigRun.ConfigRun;
import org.matsim.ServerEvSetup.SimulationInterface.SimulationBridgeInterface;
import org.matsim.run.OpenBerlinScenario;
import org.springboot.service.GenerationService.DTO.HubSpecDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SimulationRunnerService {

    private static final Logger log = LoggerFactory.getLogger(SimulationRunnerService.class);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<?> currentFuture = null;
    private SimulationState currentState = SimulationState.IDLE;
    private Exception lastException = null;
    private SimulationBridgeInterface currentSimulationBridge = null;

    @Autowired
    private SimulationPublisherService simulationPublisherService;

    /**
     * Verifica se la simulazione è in esecuzione
     */
    public synchronized boolean isRunning() {
        return currentState == SimulationState.RUNNING;
    }

    /**
     * Restituisce lo stato attuale della simulazione
     */
    public synchronized SimulationState getCurrentState() {
        return currentState;
    }

    /**
     * Restituisce l'ultima eccezione verificatasi
     */
    public synchronized Exception getLastException() {
        return lastException;
    }

    /**
     * Restituisce l'interfaccia di simulazione attuale
     */
    public synchronized SimulationBridgeInterface getCurrentSimulationBridge() {
        if (currentState != SimulationState.RUNNING) {
            return null;
        }
        return currentSimulationBridge;
    }

    /**
     * Arresta la simulazione in esecuzione
     */
    public synchronized String stop() {
        if (currentState != SimulationState.RUNNING) {
            return "Nessuna simulazione attiva.";
        }
        try {
            if (currentFuture != null) {
                currentFuture.cancel(true);
            }
            currentState = SimulationState.STOPPED;
            currentSimulationBridge = null;
            lastException = null;
            return "Richiesta di interruzione inviata.";
        } catch (Exception e) {
            log.error("Errore durante l'arresto della simulazione", e);
            currentState = SimulationState.ERROR;
            lastException = e;
            return "Errore durante l'arresto: " + e.getMessage();
        }
    }

    /**
     * Avvia la simulazione in modalità asincrona
     */
    public synchronized String runAsync(List<EvModel> evModels, List<HubSpecDto> hubSpecs, ConfigRun config) {
        if (currentState == SimulationState.RUNNING) {
            return "Simulazione già in esecuzione.";
        }
        
        // Resetta lo stato precedente
        currentState = SimulationState.RUNNING;
        lastException = null;
        currentFuture = null;

        currentFuture = executor.submit(() -> {
            try {
                runSimulation(evModels, hubSpecs, config);
                synchronized (SimulationRunnerService.this) {
                    currentState = SimulationState.COMPLETED;
                    currentSimulationBridge = null;
                    log.info("Simulazione completata con successo");
                }
            } catch (InterruptedException e) {
                synchronized (SimulationRunnerService.this) {
                    currentState = SimulationState.STOPPED;
                    currentSimulationBridge = null;
                    lastException = e;
                    log.warn("Simulazione interrotta", e);
                }
                Thread.currentThread().interrupt();
            } catch (Throwable t) {
                synchronized (SimulationRunnerService.this) {
                    currentState = SimulationState.ERROR;
                    currentSimulationBridge = null;
                    lastException = t instanceof Exception ? (Exception) t : new Exception(t);
                    log.error("Errore durante l'esecuzione della simulazione", t);
                }
            }
        });
        return "Simulazione avviata.";
    }

    /**
     * Esegue la simulazione nel thread worker
     */
    private void runSimulation(
        List<EvModel> evModels, 
        List<HubSpecDto> hubSpecs, 
        ConfigRun config
    ) throws Exception {
        OpenBerlinScenario scenarioApp = new OpenBerlinScenario().withConfigRun(config);
        SimulationBridgeInterface Bridgeinterface = scenarioApp.SetupSimulationWithServerModels(evModels, hubSpecs);
        
        synchronized (this) {
            currentSimulationBridge = Bridgeinterface;
        }
        
        simulationPublisherService.startPublisher(Bridgeinterface, config.publisherDirty(), config.publisherRateMs());
        simulationPublisherService.sendSimulationMessage("SIMULATION_START");
        scenarioApp.run();
        simulationPublisherService.sendSimulationMessage("SIMULATION_END");
        simulationPublisherService.stopPublisher();
    }
}
