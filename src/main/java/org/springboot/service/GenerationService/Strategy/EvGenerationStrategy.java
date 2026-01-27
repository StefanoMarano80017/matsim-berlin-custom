package org.springboot.service.generationService.Strategy;

import org.matsim.CustomEvModule.EVfleet.EvModel;
import org.springframework.core.io.Resource;

import java.util.List;

/**
 * Interfaccia per le strategie di generazione di modelli EV.
 * Supporta molteplici sorgenti di dati (CSV, database, API, procedurali, ecc.).
 * 
 * Implementazioni:
 * - CsvEvGenerationStrategy: legge da file CSV
 * - (future) DatabaseEvGenerationStrategy, RandomEvGenerationStrategy, ecc.
 */
public interface EvGenerationStrategy {

    /**
     * Genera una lista di modelli EV in base alla strategia implementata.
     * 
     * @param source Risorsa di input (file CSV, percorso DB, URL API, ecc.)
     * @param numberOfVehicles Numero di veicoli da generare
     * @param socMedio State of Charge medio iniziale
     * @param socStdDev Deviazione standard del SOC
     * @return Lista di EvModel generati
     */
    List<EvModel> generateEvModels(
        Resource source,
        Integer numberOfVehicles,
        Double socMedio,
        Double socStdDev
    );

    /**
     * Restituisce il nome della strategia (es. "CSV", "DATABASE", "RANDOM").
     */
    String getStrategyName();
}
