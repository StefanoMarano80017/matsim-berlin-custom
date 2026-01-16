package org.springboot.service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.matsim.CustomEvModule.EVfleet.EvModel;
import org.matsim.ServerEvSetup.ConfigRun.ConfigRun;
import org.matsim.ServerEvSetup.SimulationInterface.SimulationBridgeInterface;
import org.matsim.run.OpenBerlinScenario;
import org.springboot.service.GenerationService.DTO.HubSpecDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SimulationRunnerService {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<?> currentFuture = null;

    @Autowired
    private SimulationPublisherService simulationPublisherService;

    public synchronized boolean isRunning() {
        return currentFuture != null && !currentFuture.isDone();
    }

    public synchronized String stop() {
        if (!isRunning()) return "Nessuna simulazione attiva.";
        currentFuture.cancel(true);
        return "Richiesta di interruzione inviata.";
    }

    public synchronized String runAsync(List<EvModel> evModels, List<HubSpecDto> hubSpecs, ConfigRun config) {
        if (isRunning()) return "Simulazione già in esecuzione.";
        currentFuture = executor.submit(() -> {
            try {
                runSimulation(evModels, hubSpecs, config);
            } catch (Throwable t) {
                // log
            } 
        });
        return "Simulazione avviata.";
    }

    private void runSimulation(
        List<EvModel> evModels, 
        List<HubSpecDto> hubSpecs, 
        ConfigRun config
    ) throws Exception {
        OpenBerlinScenario scenarioApp = new OpenBerlinScenario().withConfigRun(config);
        SimulationBridgeInterface Bridgeinterface = scenarioApp.SetupSimulationWithServerModels(evModels, hubSpecs);
        simulationPublisherService.startPublisher(Bridgeinterface, false, 5000);
        simulationPublisherService.sendSimulationMessage("SIMULATION_START");
        scenarioApp.run();
        simulationPublisherService.sendSimulationMessage("SIMULATION_END");
    }
}
