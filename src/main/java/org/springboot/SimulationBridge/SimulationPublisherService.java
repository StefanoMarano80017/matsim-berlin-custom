package org.springboot.SimulationBridge;

import org.matsim.CustomMonitor.SimulationInterface.SimulationBridgeInterface;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SimulationPublisherService {

    private final SimulationBridge simulationBridge;
    private SimulationBridgeInterface simulationBridgeInterface;

    private boolean firstSnapshot = true;

    // Tempo simulato in secondi
    private double simTimeSeconds = 0.0;
    // Passo di simulazione (quanto tempo simulato passa ad ogni schedulazione)
    private final double simStepSeconds = 20.0; // 20s simulati per tick
    private final double simEndSeconds = 24 * 3600; // 24h in secondi

    public SimulationPublisherService(SimulationBridge simulationBridge) {
        this.simulationBridge = simulationBridge;
    }

    // Setter per i manager creati da CustomModule
    public void setInterface(SimulationBridgeInterface simulationBridgeInterface) {
        this.simulationBridgeInterface = simulationBridgeInterface;
        this.simTimeSeconds = 0.0; // reset del tempo simulato
        this.firstSnapshot = true;
    }

    @Scheduled(fixedRate = 5000) // ogni 5s reali
    public void publishSimulationUpdate() {
        if (simulationBridgeInterface == null) {
            return; // non ancora inizializzati
        }

        // Pubblica snapshot con il tempo simulato
        simulationBridge.publishSimulationSnapshot(simulationBridgeInterface, simTimeSeconds, firstSnapshot);
        firstSnapshot = false;

        // Incrementa il tempo simulato
        simTimeSeconds += simStepSeconds;

        // Optional: stoppa o resetta alla fine del giorno
        if (simTimeSeconds >= simEndSeconds) {
            simTimeSeconds = 0.0; firstSnapshot = true;
        }
    }
}
