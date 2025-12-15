package org.springboot.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.matsim.CustomMonitor.ConfigRun.ConfigRun;
import org.matsim.run.OpenBerlinScenario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springboot.SimulationBridge.SimulationBridge;

@Service
public class MatsimService {

    private static final Logger log = LoggerFactory.getLogger(MatsimService.class);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<?> currentFuture = null;

    @Autowired
    private SimulationBridge simulationBridge;

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

        simulationBridge.publishWsSimpleText("Simulazione Avviata");
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

        // Configurazione fluida tramite builder
        ConfigRun configRun = ConfigRun.builder()
                .csvResourceHub(new ClassPathResource("csv/charging_hub.csv"))
                .csvResourceEv(new ClassPathResource("csv/ev-dataset.csv"))
                .configPath("matsim-berlin-custom/input/v%s/berlin-v%s.config.xml")
                .sampleSizeStatic(0.001)
                .publishOnSpring(true)
                .debug(false)
                .build();

        // Crea l'istanza di scenario con la configurazione
        OpenBerlinScenario scenario = new OpenBerlinScenario().withConfigRun(configRun);
        log.info("Avvio simulazione MATSim...");
        scenario.runScenario();
        log.info("Scenario MATSim completato!");
    }

}