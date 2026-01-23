package org.springboot.service.SimulationState;

import org.matsim.ServerEvSetup.SimulationInterface.SimulationBridgeInterface;

public interface SimulationStateListener {
    void onSimulationStarted(SimulationBridgeInterface bridge);
    void onSimulationEnded();
}