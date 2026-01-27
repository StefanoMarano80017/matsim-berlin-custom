package org.springboot.service.simulationState;

import org.matsim.ServerEvSetup.SimulationInterface.SimulationBridgeInterface;

public interface SimulationStateListener {
    void onSimulationStarted(SimulationBridgeInterface bridge);
    void onSimulationEnded();
}