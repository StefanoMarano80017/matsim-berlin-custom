package org.matsim.ServerEvSetup.SimulationInterface;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.Controler;

public class SimulationHandler {
    private final SimulationBridgeInterface bridge;
    private final Controler controler;
    private final Scenario scenario;

    public SimulationHandler(
        SimulationBridgeInterface bridge,
        Controler controler, 
        Scenario  scenario
    ) {
        this.bridge    = bridge;
        this.controler = controler;
        this.scenario  = scenario;
    }

    public SimulationBridgeInterface getInterface() {
        return bridge;
    }

    public Scenario getScenario(){
        return scenario;
    }

    public void run() {
        controler.run();
    }   
}
