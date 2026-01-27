package org.matsim.ServerEvSetup.SimulationInterface.Service;

import java.util.List;

import org.matsim.CustomEvModule.EVfleet.EvFleetManager;
import org.matsim.CustomEvModule.EVfleet.EvModel;
import org.matsim.CustomEvModule.Hub.HubManager;
import org.springboot.service.GenerationService.DTO.HubSpecDto;
import org.springframework.util.CollectionUtils;

public class SimulationBootstrapService {

    private final HubManager     hubManager;
    private final EvFleetManager evFleetManager;

    public SimulationBootstrapService(
        HubManager hubManager,
        EvFleetManager evFleetManager
    ) {
        this.hubManager = hubManager;
        this.evFleetManager = evFleetManager;
    }

    /**
     * Riceve i modelli EV pre-generati dal server e li registra nel manager.
     * Questo metodo è chiamato prima della simulazione.
     * 
     * @param evModels Lista di EvModel già generati dal server
     */
    public void initializeEvModels(List<EvModel> evModels) {
        if (CollectionUtils.isEmpty(evModels)) {
            throw new IllegalArgumentException("evModels cannot be null or empty");
        }
        evFleetManager.registerEvModels(evModels);
    }

    /**
     * Riceve gli hub di ricarica pre-generati dal server e li registra nel manager.
     * Questo metodo è chiamato prima della simulazione.
     * 
     * Nota: Accetta i modelli di dominio puri (HubSpecDto) generati dal server,
     * NON le specifiche MATSim. La registrazione nell'infrastruttura viene fatta
     * dal manager.
     * 
     * @param hubSpecs Lista di specifiche hub (modelli di dominio server)
     */
    public void initializeChargingHubs(List<HubSpecDto> hubSpecs) {
        if (CollectionUtils.isEmpty(hubSpecs)) {
            throw new IllegalArgumentException("hubSpecs cannot be null or empty");
        }
        hubManager.registerChargingHubsFromSpecs(hubSpecs);
    }
}
