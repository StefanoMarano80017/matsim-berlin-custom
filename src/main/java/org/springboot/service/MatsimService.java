package org.springboot.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.matsim.run.OpenBerlinScenario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springboot.websocket.SimulationBridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class MatsimService {

    private static final Logger log = LoggerFactory.getLogger(MatsimService.class);

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
                t.printStackTrace();
            }
        });

        simulationBridge.publishSimpleText("Simulazione Avviata");
        return "Simulazione avviata.";
    }

    /**
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
        OpenBerlinScenario.setConfigPath("matsim-berlin-custom/input/v%s/berlin-v%s.config.xml");
        OpenBerlinScenario.setSpringContext(this.applicationContext);
        OpenBerlinScenario.setHubCSV(new ClassPathResource("csv/charging_hub.csv"));
        OpenBerlinScenario.setEvCSV(new ClassPathResource("csv/ev-dataset.csv"));
        log.info("Avvio simulazione MATSim...");
        // MATSimApplication.run può lanciare una RuntimeException o una ExecutionException
        // che verrà catturata nel blocco runThread.
        OpenBerlinScenario scenario = new OpenBerlinScenario();
        scenario.runScenario(0.001);
        log.info("Scenario MATSim completato!");
    }

}