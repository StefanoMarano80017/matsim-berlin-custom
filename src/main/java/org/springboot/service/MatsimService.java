package org.springboot.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.matsim.application.MATSimApplication;
import org.matsim.run.OpenBerlinScenario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springboot.websocket.SimulationBridge;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j(topic = "org.springboot")
public class MatsimService {

    private final SimulationBridge simulationBridge;
    private final ApplicationContext applicationContext;
    
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<?> currentFuture = null;

    @Autowired
    public MatsimService(SimulationBridge simulationBridge, ApplicationContext applicationContext) {
        this.simulationBridge = simulationBridge;
        this.applicationContext = applicationContext;
    }

    /**
     * Avvia il thread della simulazione.
     * @return Stato dell'avvio (successo o già in esecuzione).
     */
    public String runThread(){
        if (currentFuture != null && !currentFuture.isDone()) {
            return "Simulazione già in esecuzione.";
        }

        currentFuture = executor.submit(() -> {
            try {
                runScenario();
            } catch (Throwable t) {
                log.warn("Simulazione interrotta: {}", t.getMessage());
            }
        });

        simulationBridge.publishSimpleText("Simulazione Avviata");
        return "Simulazione avviata.";
    }

    /**
     *  NON è SAFE PER ORA
     * Tenta di arrestare la simulazione in esecuzione.
     * @return Stato del tentativo di arresto.
     */
    public String shutdownThread(){
        if (currentFuture == null || currentFuture.isDone()) {
            return "Nessuna simulazione attiva.";
        }

        currentFuture.cancel(true); // tenta di interrompere
        return "Richiesta di interruzione inviata.";
    }

    /**
     * Logica principale di esecuzione MATSim.
     * @throws Exception Se MATSimApplication fallisce.
     */
    public void runScenario() throws Exception {
        log.info("Preparazione scenario MATSim...");
        OpenBerlinScenario.setSpringContext(this.applicationContext);
        String[] args = new String[] { 
            "run", 
            "--1pct" 
        };

        log.info("Avvio simulazione MATSim...");
        // MATSimApplication.run può lanciare una RuntimeException o una ExecutionException
        // che verrà catturata nel blocco runThread.
        MATSimApplication.run(OpenBerlinScenario.class, args);
        log.info("Scenario MATSim completato!");
    }

}