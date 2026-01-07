package org.matsim.ServerEvSetup.SimulationInterface;

import java.util.List;

import org.matsim.CustomEvModule.EVfleet.EvModel;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.Controler;
import org.springboot.DTO.SimulationDTO.EvFleetDto;

public class SimulationHandler {

    private final SimulationBridgeInterface bridge;
    private final Controler controler;
    private final Scenario scenario;
    private final EvFleetDto fleetDto;

    public SimulationHandler(
        List<EvModel> evModels,
        SimulationBridgeInterface bridge,
        Controler controler, 
        Scenario  scenario
    ) {
        this.bridge    = bridge;
        this.controler = controler;
        this.scenario  = scenario;
        this.fleetDto  = new EvFleetDto(evModels);
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

    public void run() {
        controler.run();
    }   
}
