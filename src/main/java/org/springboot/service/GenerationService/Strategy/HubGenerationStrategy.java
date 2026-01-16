package org.springboot.service.GenerationService.Strategy;

import org.springboot.service.GenerationService.DTO.HubSpecDto;
import org.springframework.core.io.Resource;

import java.util.List;

/**
 * Interfaccia per le strategie di generazione di specifiche hub di ricarica.
 * Supporta molteplici sorgenti di dati (CSV, database, API, procedurali, ecc.).
 * 
 * Genera modelli di dominio puri (HubSpecDto), indipendenti da MATSim.
 * 
 * Implementazioni:
 * - CsvHubGenerationStrategy: legge da file CSV
 * - (future) DatabaseHubGenerationStrategy, RandomHubGenerationStrategy, ecc.
 */
public interface HubGenerationStrategy {

    /**
     * Genera una lista di specifiche hub di ricarica in base alla strategia implementata.
     * 
     * @param source Risorsa di input (file CSV, percorso DB, URL API, ecc.)
     * @return Lista di HubSpecDto generati
     */
    List<HubSpecDto> generateHubSpecifications(Resource source);

    /**
     * Restituisce il nome della strategia (es. "CSV", "DATABASE", "RANDOM").
     */
    String getStrategyName();
}
