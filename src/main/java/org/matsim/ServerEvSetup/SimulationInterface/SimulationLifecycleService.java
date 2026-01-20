package org.matsim.ServerEvSetup.SimulationInterface;

public class SimulationLifecycleService {

    private volatile boolean simulationStarted = false;
    private volatile double currentSimTime = 0.0;

    public void setsimulationStarted(boolean isStated){
        simulationStarted = isStated;
    }

    public boolean isSimulationStarted() {
        return simulationStarted;
    }

    /**
     * Imposta il timestep REALE della simulazione.
     * Chiamato dai Monitoring quando ricevono gli eventi di MATSim.
     * 
     * @param simTime Il tempo di simulazione reale in secondi
     */
    public void setCurrentSimTime(double simTime) {
        this.currentSimTime = simTime;
    }

    public double getCurrentSimTime() {
        return currentSimTime;
    }
}
