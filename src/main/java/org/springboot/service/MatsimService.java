package org.springboot.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springboot.DTO.SimulationDTO.EvFleetDto;
import org.springboot.DTO.SimulationDTO.HubListDTO;
import org.springboot.DTO.SimulationDTO.SimulationSettingsDTO;
import org.springboot.SimulationBridge.SimulationPublisherService;

import org.matsim.ServerEvSetup.ConfigRun.ConfigRun;
import org.matsim.ServerEvSetup.SimulationInterface.SimulationHandler;
import org.matsim.run.OpenBerlinScenario;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class MatsimService {

    private static final Logger log = LoggerFactory.getLogger(MatsimService.class);

    // ======= Threading & Async =======
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<?> currentFuture = null;

    // ======= Dependencies =======
    @Autowired
    private SimulationPublisherService simulationPublisherService;

    // ======= Simulation state =======
    private volatile SimulationHandler simulationHandler;
    private volatile boolean isReady = false;

    // ===============================
    // ======= Public API ============
    // ===============================

    /**
     * Avvia la simulazione in background.
     */
    public synchronized String runSimulationAsync(SimulationSettingsDTO settings) {
        if (isSimulationRunning()) {
            return "Simulazione già in esecuzione.";
        }

        currentFuture = executor.submit(() -> {
            try {
                runSimulation(settings);
            } catch (Throwable t) {
                log.warn("Simulazione interrotta: {}", t.getMessage(), t);
            } finally {
                simulationHandler = null;
                isReady = false;
            }
        });

        return "Simulazione avviata.";
    }

    /**
     * Verifica se la simulazione è attiva.
     */
    public boolean isSimulationRunning() {
        return currentFuture != null && !currentFuture.isDone();
    }

    /**
     * Richiesta di interruzione della simulazione.
     */
    public synchronized String stopSimulation() {
        if (!isSimulationRunning()) {
            return "Nessuna simulazione attiva.";
        }

        currentFuture.cancel(true);
        return "Richiesta di interruzione inviata.";
    }

    /**
     * Recupera informazioni sulla flotta EV.
     */
    public EvFleetDto getVehiclesInfo() {
        return simulationHandler != null ? simulationHandler.getEvFleetDto() : null;
    }

    /**
     * Recupera informazioni sugli hub.
     */
    public HubListDTO getHubsInfo() {
        return simulationHandler != null ? simulationHandler.getHubListDTO() : null;
    }

    // ===============================
    // ======= Simulation Lifecycle ==
    // ===============================
    private void runSimulation(SimulationSettingsDTO settings) throws Exception {
        if (!isReady) {
            setupScenario(settings);
        } else {
            log.info("Scenario già pronto, avvio diretto...");
        }

        log.info("Avvio simulazione MATSim...");

        // --- Notifica inizio simulazione ---
        simulationPublisherService.sendSimulationMessage("SIMULATION_START");

        // --- Avvio publisher ---
        simulationPublisherService.startPublisher(
                simulationHandler.getInterface(),
                false, // dirty snapshot = false = full snapshot
                5000   // frequenza in ms
        );

        // --- Avvio simulazione ---
        simulationHandler.run();

        log.info("Scenario MATSim completato!");

        // --- Notifica fine simulazione ---
        simulationPublisherService.sendSimulationMessage("SIMULATION_END");
    }

    // ===============================
    // ======= Scenario Setup ========
    // ===============================

    /**
     * Setup scenario MATSim di default (demo/test).
     */
    public void setupScenario() throws Exception {
        log.info("Preparazione scenario MATSim default...");

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
                .debug(true)
                .build();

        initializeScenario(configRun);
    }

    /**
     * Setup scenario MATSim da DTO custom.
     */
    public void setupScenario(SimulationSettingsDTO settings) throws Exception {
        log.info("Preparazione scenario MATSim con parametri custom...");

        ConfigRun configRun = ConfigRun.builder()
                .csvResourceHub(new ClassPathResource(settings.getCsvResourceHub()))
                .csvResourceEv(new ClassPathResource(settings.getCsvResourceEv()))
                .configPath(settings.getConfigPath())
                .vehicleStrategy(settings.getVehicleStrategy())
                .planStrategy(settings.getPlanStrategy())
                .sampleSizeStatic(settings.getSampleSizeStatic())
                .stepSize(settings.getStepSize())
                .numeroVeicoli(settings.getNumeroVeicoli())
                .socMedio(settings.getSocMedio())
                .socStdDev(settings.getSocStdDev())
                .targetSocMean(settings.getTargetSocMean())
                .targetSocStdDev(settings.getTargetSocStdDev())
                .debug(settings.getDebug())
                .build();

        initializeScenario(configRun);
    }

    /**
     * Helper comune per inizializzare lo scenario.
     */
    private void initializeScenario(ConfigRun configRun) throws Exception {
        this.simulationHandler = new OpenBerlinScenario()
                .withConfigRun(configRun)
                .SetupSimulation();
        this.isReady = true;
    }
}
