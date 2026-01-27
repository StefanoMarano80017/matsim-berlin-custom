package org.springboot.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springboot.DTO.SimulationDTO.SimulationSettingsDTO;
import org.springboot.DTO.SimulationDTO.EvFleetDto;
import org.springboot.DTO.SimulationDTO.HubDTO;
import org.springboot.DTO.SimulationDTO.HubListDTO;
import org.springboot.DTO.SimulationDTO.mapper.HubSpecMapper;
import org.springboot.service.GenerationService.ModelGenerationService;
import org.springboot.service.GenerationService.DTO.HubSpecDto;
import org.springboot.service.SimulationState.SimulationState;
import org.springboot.service.SimulationState.SimulationStateListener;
import org.matsim.CustomEvModule.EVfleet.EvModel;
import org.matsim.ServerEvSetup.ConfigRun.ConfigRun;
import org.matsim.ServerEvSetup.SimulationInterface.SimulationBridgeInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class MatsimService {

    private static final Logger log = LoggerFactory.getLogger(MatsimService.class);

    // ======= Dependencies =======
    @Autowired
    private ModelGenerationService modelGenerationService;

    @Autowired
    private SimulationRunnerService runnerService;
    
    @Autowired
    private SimulationUpdaterService updaterService;

    @Autowired
    private SimulationPublisherService simulationPublisherService;

    // ======= Generation state (server-side models) =======
    private volatile List<EvModel> generatedEvModels = null;
    private volatile List<HubSpecDto> generatedHubSpecs = null;

    // =================================================
    // ======= API Controllo simulazione    ============
    // =================================================
    /**
     * Avvia la simulazione in background.
     */
    public String runSimulationAsync(SimulationSettingsDTO settings) {
        if(generatedEvModels == null || generatedHubSpecs == null){
            String status = "Errore: ";
            if (generatedEvModels == null) status += "flotta non generata ";
            if (generatedHubSpecs == null) status += "hub non generati";
            return status;
        }
        ConfigRun config = buildConfigRun(settings);

        runnerService.setSimulationStateListener(new SimulationStateListener() {
            @Override
            public void onSimulationStarted(SimulationBridgeInterface bridge) {
                simulationPublisherService.sendSimulationMessage("SIMULATION_START");
                simulationPublisherService.startPublisher(
                    bridge, config.publisherDirty(), config.publisherRateMs()
                );
            }

            @Override
            public void onSimulationEnded() {
                simulationPublisherService.stopPublisher();
                simulationPublisherService.sendSimulationMessage("SIMULATION_END");
            }
        });

        return runnerService.runAsync(generatedEvModels, generatedHubSpecs, config);
    }

    /**
     * Verifica se la simulazione è attiva.
     */
    public boolean isSimulationRunning() {
        return runnerService.isRunning();
    }

    /**
     * Restituisce lo stato attuale della simulazione.
     */
    public SimulationState getSimulationState() {
        return runnerService.getCurrentState();
    }

    /**
     * Restituisce l'ultima eccezione verificatasi nella simulazione.
     */
    public Exception getSimulationException() {
        return runnerService.getLastException();
    }

    /**
    * Richiesta di interruzione della simulazione.
    */
    public String stopSimulation() {
        return runnerService.stop();
    }

    public String updateChargerState(String chargerId, boolean isActive) {
        String result = runnerService.mapCurrentSimulationBridge(bridge -> {
            String res = updaterService.setChargerState(bridge, chargerId, isActive);
            log.info(
                "[MatsimService] Stato cambiato al charger: {} con esito: {}",
                chargerId,
                res
            );
            return res;
        });
        return result != null ? result : "Simulazione non in esecuzione";
    }

    // ===============================
    // ======= Generation API ========
    // ===============================
    /**
     * Genera i modelli EV lato server.
     * Legge il CSV e crea EvModel puri, indipendenti da MATSim.
     */
    public String generateFleet(
        String csvResourceEv, 
        Integer numeroVeicoli, 
        Double socMedio, 
        Double socStdDev
    ) {
        try {
            log.info("[GenerationAPI] Generating fleet from {}", csvResourceEv);
            this.generatedEvModels = modelGenerationService.generateEvModels(
                new ClassPathResource(csvResourceEv),
                numeroVeicoli,
                socMedio,
                socStdDev
            );
            log.info("[GenerationAPI] Generated {} EV models", generatedEvModels.size());
            return "Flotta generata: " + generatedEvModels.size() + " veicoli.";
        } catch (Exception e) {
            log.error("[GenerationAPI] Errore nella generazione della flotta", e);
            this.generatedEvModels = null;
            throw new RuntimeException("Errore nella generazione della flotta: " + e.getMessage());
        }
    }

    /**
     * Genera i modelli degli hub lato server.
     * Legge il CSV e crea HubSpecDto puri, indipendenti da MATSim.
     */
    public String generateHubs(String csvResourceHub) {
        try {
            log.info("[GenerationAPI] Generating hubs from {}", csvResourceHub);
            this.generatedHubSpecs = modelGenerationService.generateHubSpecifications(new ClassPathResource(csvResourceHub));
            log.info("[GenerationAPI] Generated {} hub specifications", generatedHubSpecs.size());
            return "Hub generati: " + generatedHubSpecs.size() + " hub.";
        } catch (Exception e) {
            log.error("[GenerationAPI] Errore nella generazione degli hub", e);
            this.generatedHubSpecs = null;
            throw new RuntimeException("Errore nella generazione degli hub: " + e.getMessage());
        }
    }

    /**
     * Recupera i modelli EV generati dal server.
     */
    public EvFleetDto getGeneratedFleet() {
        return isFleetGenerated() ? new EvFleetDto(generatedEvModels) : null;
    }

    /**
     * Recupera i modelli degli hub generati dal server.
     * Converte HubSpecDto → HubDTO tramite HubSpecMapper.
     */
    public HubListDTO getGeneratedHubs() {
        List<HubDTO> hubDtos = generatedHubSpecs.stream().map(HubSpecMapper::toHubDTO).toList();
        return areHubsGenerated() ? new HubListDTO(hubDtos) : null;
    }

    /**
     * Verifica se la flotta è stata generata.
     */
    public boolean isFleetGenerated() {
        return generatedEvModels != null && !generatedEvModels.isEmpty();
    }

    /**
     * Verifica se gli hub sono stati generati.
     */
    public boolean areHubsGenerated() {
        return generatedHubSpecs != null && !generatedHubSpecs.isEmpty();
    }

    /**
     * Helper per costruire ConfigRun da SimulationSettingsDTO.
     */
    private ConfigRun buildConfigRun(SimulationSettingsDTO settings) {
        return ConfigRun.builder()
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
                .debugLink(settings.getDebugLink())
                .realTime(settings.getRealTime())
                .publisherDirty(settings.isPublisherDirty())
                .publisherRateMs(settings.getPublisherRateMs())
                .build();
    }

}
