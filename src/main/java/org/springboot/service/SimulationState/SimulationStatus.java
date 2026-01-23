package org.springboot.service.SimulationState;

import org.matsim.ServerEvSetup.SimulationInterface.SimulationBridgeInterface;

public class SimulationStatus {
    private SimulationState state = SimulationState.IDLE;
    private SimulationBridgeInterface bridge;
    private Exception lastException;
    private SimulationStateListener listener;

    public synchronized SimulationState getState() { return state; }
    public synchronized SimulationBridgeInterface getBridge() { return bridge; }
    public synchronized Exception getLastException() { return lastException; }

    public synchronized void setListener(SimulationStateListener listener) {
        this.listener = listener;
    }

    public synchronized void update(
            SimulationState newState,
            SimulationBridgeInterface newBridge,
            Exception exception,
            boolean notifyListener
    ) {
        this.state = newState;
        this.bridge = newBridge;
        this.lastException = exception;

        if (listener != null && notifyListener) {
            switch (newState) {
                case RUNNING -> listener.onSimulationStarted(newBridge);
                case COMPLETED, STOPPED, ERROR, IDLE -> listener.onSimulationEnded();
            }
        }
    }

    public synchronized void reset() {
        this.state = SimulationState.IDLE;
        this.bridge = null;
        this.lastException = null;
    }
}