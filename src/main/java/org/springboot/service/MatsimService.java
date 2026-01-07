package org.springboot.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springboot.DTO.SimulationDTO.EvFleetDto;
import org.springboot.SimulationBridge.SimulationPublisherService;

import org.matsim.ServerEvSetup.ConfigRun.ConfigRun;
import org.matsim.ServerEvSetup.SimulationInterface.SimulationHandler;
import org.matsim.run.OpenBerlinScenario;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class MatsimService {

    private static final Logger log = LoggerFactory.getLogger(MatsimService.class);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<?> currentFuture = null;

    @Autowired
    private SimulationPublisherService simulationPublisherService;
    private volatile SimulationHandler simulationHandler; // riferimento alla simulazione in corso
    private volatile boolean isReady = false;

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
            } finally {
                simulationHandler = null; // pulizia
            }
        });

        return "Simulazione avviata.";
    }

    public boolean isSimulationRunning() {
        return (currentFuture != null && !currentFuture.isDone());
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
    public void setupScenario() throws Exception {
        log.info("Preparazione scenario MATSim...");
        // Configurazione fluida tramite builder
        ConfigRun configRun = ConfigRun.builder()
                .csvResourceHub(new ClassPathResource("csv/charging_hub.csv"))
                .csvResourceEv(new ClassPathResource("csv/ev-dataset.csv"))
                .configPath("input/v%s/berlin-v%s.config.xml")
                .vehicleStrategy(ConfigRun.VehicleGenerationStrategyEnum.FROM_CSV)
                .planStrategy(ConfigRun.PlanGenerationStrategyEnum.STATIC)
                .sampleSizeStatic(0.001)
                .stepSize(900.0)
                .numeroVeicoli(2)
                .socMedio(0.70)
                .socStdDev(0.05)
                .targetSocMean(0.90)
                .targetSocStdDev(0.05)
                .publishOnSpring(true)
                .debug(true)
                .build();

        // Crea l'istanza di scenario con la configurazione
        this.simulationHandler = new OpenBerlinScenario().withConfigRun(configRun).SetupSimulation();
        this.isReady = true;
    }

    public void runScenario() throws Exception{
        if(isReady) {
            log.info("Scenario già pronto, avvio diretto...");
        } else {
            setupScenario();
        }

        log.info("Avvio simulazione MATSim...");
        simulationPublisherService.setInterface(this.simulationHandler.getInterface());
        this.simulationHandler.run();
        log.info("Scenario MATSim completato!");
    }

    public EvFleetDto getVehiclesInfo() {
        if (this.simulationHandler == null) {
            return null;
        }
        return this.simulationHandler.getEvFleetDto();
    }

}