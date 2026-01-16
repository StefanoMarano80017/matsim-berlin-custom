package org.springboot.service.GenerationService;

import org.matsim.CustomEvModule.EVfleet.EvModel;
import org.springboot.service.GenerationService.DTO.HubSpecDto;
import org.springboot.service.GenerationService.Strategy.EvGenerationStrategy;
import org.springboot.service.GenerationService.Strategy.HubGenerationStrategy;
import org.springboot.service.GenerationService.Strategy.Impl.CsvEvGenerationStrategy;
import org.springboot.service.GenerationService.Strategy.Impl.CsvHubGenerationStrategy;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Servizio di generazione modelli lato server.
 * 
 * Responsabilità:
 * - Generare modelli EV (EvModel)
 * - Generare specifiche hub (HubSpecDto - modelli di dominio puri, indipendenti da MATSim)
 * - Fornire un unico punto di ingresso per la generazione dei dati
 * - Supportare molteplici strategie di generazione (CSV, DB, API, ecc.)
 * 
 * Flusso:
 * 1. Server (questo servizio) genera i modelli
 * 2. I modelli vengono passati al bridge
 * 3. Il bridge li passa alla simulazione
 * 4. La simulazione traduce HubSpecDto in MATSim specifiche e registra
 */
@Service
public class ModelGenerationService {

    private static final Logger log = LogManager.getLogger(ModelGenerationService.class);

    private final EvGenerationStrategy evGenerationStrategy;
    private final HubGenerationStrategy hubGenerationStrategy;

    public ModelGenerationService() {
        // Per ora utilizza solo CSV. In futuro sarà configurabile via Strategy Factory
        this.evGenerationStrategy = new CsvEvGenerationStrategy();
        this.hubGenerationStrategy = new CsvHubGenerationStrategy();
    }

    /**
     * Genera una lista di modelli EV.
     * 
     * @param csvResource Risorsa CSV con i dati dei veicoli
     * @param numberOfVehicles Numero di veicoli da generare
     * @param socMedio State of Charge medio iniziale
     * @param socStdDev Deviazione standard del SOC
     * @return Lista di EvModel generati
     */
    public List<EvModel> generateEvModels(
        Resource csvResource,
        Integer numberOfVehicles,
        Double socMedio,
        Double socStdDev
    ) {
        log.info("[ModelGenerationService] Generating EV models using strategy: {}", 
            evGenerationStrategy.getStrategyName());

        List<EvModel> models = evGenerationStrategy.generateEvModels(
            csvResource,
            numberOfVehicles,
            socMedio,
            socStdDev
        );

        log.info("[ModelGenerationService] Generated {} EV models", models.size());
        return models;
    }

    /**
     * Genera una lista di specifiche hub di ricarica (modelli di dominio puri).
     * 
     * Questi modelli sono indipendenti da MATSim e contengono solo le informazioni
     * essenziali per:
     * - trasportare tramite il bridge
     * - tradurre in ChargingHub e ImmutableChargerSpecification lato simulazione
     * 
     * @param csvResource Risorsa CSV con i dati degli hub
     * @return Lista di HubSpecDto generati
     */
    public List<HubSpecDto> generateHubSpecifications(Resource csvResource) {
        log.info("[ModelGenerationService] Generating hub specifications using strategy: {}", 
            hubGenerationStrategy.getStrategyName());

        List<HubSpecDto> specs = hubGenerationStrategy.generateHubSpecifications(csvResource);

        log.info("[ModelGenerationService] Generated {} hub specifications", specs.size());
        return specs;
    }
}
