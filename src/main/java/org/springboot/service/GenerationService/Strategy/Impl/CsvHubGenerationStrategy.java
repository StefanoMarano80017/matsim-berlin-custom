package org.springboot.service.GenerationService.Strategy.Impl;

import org.springboot.service.GenerationService.DTO.HubSpecDto;
import org.springboot.service.GenerationService.Parser.HubCsvParser;
import org.springboot.service.GenerationService.Strategy.HubGenerationStrategy;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Implementazione CSV per la generazione di specifiche hub di ricarica.
 * 
 * Usa HubCsvParser per leggere il CSV e generare modelli di dominio puri (HubSpecDto).
 * Non dipende da MATSim.
 */
@Component
public class CsvHubGenerationStrategy implements HubGenerationStrategy {

    private final HubCsvParser parser = new HubCsvParser();

    @Override
    public List<HubSpecDto> generateHubSpecifications(Resource source) {
        if (source == null) {
            throw new IllegalArgumentException("CSV resource cannot be null");
        }

        try {
            return parser.parseHubs(source);
        } catch (IOException e) {
            throw new RuntimeException("Errore durante il parsing del CSV degli hub", e);
        }
    }

    @Override
    public String getStrategyName() {
        return "CSV";
    }
}

