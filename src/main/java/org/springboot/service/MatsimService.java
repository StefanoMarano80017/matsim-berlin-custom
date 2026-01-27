package org.springboot.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springboot.DTO.in.SimulationSettingsDTO;
import org.springboot.DTO.out.SimulationDTO.EvFleetDto;
import org.springboot.DTO.out.SimulationDTO.HubDTO;
import org.springboot.DTO.out.SimulationDTO.HubListDTO;
import org.springboot.DTO.out.SimulationDTO.mapper.HubSpecMapper;
import org.springboot.service.generationService.ModelGenerationService;
import org.springboot.service.generationService.DTO.HubSpecDto;
import org.springboot.service.result.ChargerStateUpdateResult;
import org.springboot.service.result.GenerationResult;
import org.springboot.service.result.SimulationStartResult;
import org.springboot.service.result.SimulationStopResult;
import org.springboot.service.simulationState.SimulationState;
import org.springboot.service.simulationState.SimulationStateListener;
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
    public SimulationStartResult runSimulationAsync(SimulationSettingsDTO settings) {
        if(isSimulationRunning()){
            return SimulationStartResult.ALREADY_RUNNING;
        } 

        if (generatedEvModels == null && generatedHubSpecs == null) {
            log.error("[MatsimService] Flotta e hub non generati");
            return SimulationStartResult.FLEET_AND_HUBS_NOT_GENERATED;
        } else if (generatedEvModels == null) {
            log.error("[MatsimService] Flotta non generata");
            return SimulationStartResult.FLEET_NOT_GENERATED;
        } else if (generatedHubSpecs == null) {
            log.error("[MatsimService] Hub non generati");
            return SimulationStartResult.HUBS_NOT_GENERATED;
        }

        try {
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

            runnerService.runAsync(generatedEvModels, generatedHubSpecs, config);
            log.info("[MatsimService] Simulazione avviata con successo");
            return SimulationStartResult.SUCCESS;
        } catch (Exception e) {
            log.error("[MatsimService] Errore nell'avvio della simulazione", e);
            return SimulationStartResult.ERROR;
        }
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
    public SimulationStopResult stopSimulation() {
        if (!isSimulationRunning()) {
            SimulationState currentState = getSimulationState();
            log.warn("[MatsimService] Tentativo di arresto quando simulazione non è in esecuzione. Stato attuale: {}", currentState);
            return SimulationStopResult.NOT_RUNNING;
        }

        try {
            runnerService.stop();
            log.info("[MatsimService] Richiesta di interruzione inviata con successo");
            return SimulationStopResult.SUCCESS;
        } catch (Exception e) {
            log.error("[MatsimService] Errore nell'arresto della simulazione", e);
            return SimulationStopResult.ERROR;
        }
    }

    public ChargerStateUpdateResult updateChargerState(String chargerId, boolean isActive) {
        if (!isSimulationRunning()) {
            log.warn("[MatsimService] Tentativo di aggiornare charger quando simulazione non è in esecuzione");
            return ChargerStateUpdateResult.SIMULATION_NOT_RUNNING;
        }

        boolean isChargerExist = this.generatedHubSpecs.stream().anyMatch(hub -> hub.hasCharger(chargerId));
        if(!isChargerExist) return ChargerStateUpdateResult.INVALID_CHARGER_ID;
        return runnerService.mapCurrentSimulationBridge(bridge -> {
            ChargerStateUpdateResult res = updaterService.setChargerState(bridge, chargerId, isActive);
            log.info(
                "[MatsimService] Stato cambiato al charger: {} con esito: {}",
                chargerId,
                res
            );
            return res;
        });

    }

    // ===============================
    // ======= Generation API ========
    // ===============================
    /**
     * Genera i modelli EV lato server.
     * Legge il CSV e crea EvModel puri, indipendenti da MATSim.
     */
    public GenerationResult generateFleet(
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
            return GenerationResult.SUCCESS;
        } catch (Exception e) {
            log.error("[GenerationAPI] Errore nella generazione della flotta", e);
            this.generatedEvModels = null;
            return GenerationResult.ERROR;
        }
    }

    /**
     * Genera i modelli degli hub lato server.
     * Legge il CSV e crea HubSpecDto puri, indipendenti da MATSim.
     */
    public GenerationResult generateHubs(String csvResourceHub) {
        try {
            log.info("[GenerationAPI] Generating hubs from {}", csvResourceHub);
            this.generatedHubSpecs = modelGenerationService.generateHubSpecifications(new ClassPathResource(csvResourceHub));
            log.info("[GenerationAPI] Generated {} hub specifications", generatedHubSpecs.size());
            return GenerationResult.SUCCESS;
        } catch (Exception e) {
            log.error("[GenerationAPI] Errore nella generazione degli hub", e);
            this.generatedHubSpecs = null;
            return GenerationResult.ERROR;
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
