package org.springboot.service.GenerationService.Strategy.Impl;

import org.matsim.CustomEvModule.EVfleet.EvModel;
import org.matsim.CustomEvModule.EVfleet.strategy.fleet.EvFleetStrategy;
import org.springboot.service.GenerationService.Strategy.EvGenerationStrategy;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implementazione CSV per la generazione di modelli EV.
 * Delega la logica alla strategia MATSim esistente CsvFleetGenerationStrategy.
 */
@Component
public class CsvEvGenerationStrategy implements EvGenerationStrategy {

    private final EvFleetStrategy csvFleetStrategy;

    public CsvEvGenerationStrategy() {
        // Istanzia la strategia CSV da MATSim
        this.csvFleetStrategy = new org.matsim.CustomEvModule.EVfleet.strategy.fleet.CsvFleetGenerationStrategy();
    }

    @Override
    public List<EvModel> generateEvModels(
        Resource source,
        Integer numberOfVehicles,
        Double socMedio,
        Double socStdDev
    ) {
        if (source == null) {
            throw new IllegalArgumentException("CSV resource cannot be null");
        }

        return csvFleetStrategy.generateFleet(
            source,
            numberOfVehicles,
            socMedio,
            socStdDev
        );
    }

    @Override
    public String getStrategyName() {
        return "CSV";
    }
}
