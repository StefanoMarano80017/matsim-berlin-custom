package org.matsim.ServerEvSetup.SimulationInterface;

import java.util.Collection;
import java.util.List;

import org.matsim.CustomEvModule.EVfleet.EvModel;
import org.matsim.CustomEvModule.Hub.ChargingHub;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.Controler;
import org.springboot.DTO.SimulationDTO.EvFleetDto;
import org.springboot.DTO.SimulationDTO.HubListDTO;

public class SimulationHandler {

    private final SimulationBridgeInterface bridge;
    private final Controler controler;
    private final Scenario scenario;
    //DTOs
    private final EvFleetDto fleetDto;
    private final HubListDTO hubListDTO;

    public SimulationHandler(
        List<EvModel> evModels,
        Collection<ChargingHub> chargingHubs,
        SimulationBridgeInterface bridge,
        Controler controler, 
        Scenario  scenario
    ) {
        this.bridge    = bridge;
        this.controler = controler;
        this.scenario  = scenario;
        this.fleetDto  = new EvFleetDto(evModels);
        this.hubListDTO = new HubListDTO(chargingHubs);
    }

    public SimulationBridgeInterface getInterface() {
        return bridge;
    }

    public Scenario getScenario(){
        return scenario;
    }

    public EvFleetDto getEvFleetDto() {
        return fleetDto;
    }

    public HubListDTO getHubListDTO() {
        return hubListDTO;
    }

    public void run() {
        controler.run();
    }   
}
