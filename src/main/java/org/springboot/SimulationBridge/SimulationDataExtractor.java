package org.springboot.SimulationBridge;

import org.matsim.ServerEvSetup.SimulationInterface.SimulationBridgeInterface;
import org.springboot.DTO.WebSocketDTO.payload.TimeStepPayload;
import org.springframework.stereotype.Component;

/**
 * Responsabilità: Estrazione e conversione dati dalla simulazione.
 * 
 * Prende i dati grezzi da SimulationBridgeInterface e li converte
 * in DTO pronti per il publishing via WebSocket.
 * 
 * Questa classe NON conosce della pubblicazione WebSocket,
 * si limita a estrarre e organizzare i dati.
 */
@Component
public class SimulationDataExtractor {

    /**
     * Estrae lo stato della simulazione dal bridge e lo converte in TimeStepPayload.
     * 
     * @param simulationBridgeInterface Il bridge da cui estrarre i dati
     * @param fullSnapshot true = invia tutto, false = solo elementi dirty
     * @return TimeStepPayload con i dati, o null se non ci sono dati da inviare
     */
    public TimeStepPayload extractTimeStepSnapshot(
            SimulationBridgeInterface simulationBridgeInterface,
            boolean fullSnapshot
    ) {
        if (simulationBridgeInterface == null) {
            return null;
        }

        // Delega a SimulationBridgeInterface l'estrazione dei dati grezzi
        TimeStepPayload payload = simulationBridgeInterface.GetTimeStepStatus(fullSnapshot);
        
        // Se non ci sono dati, ritorna null
        if (payload == null) {
            return null;
        }

        // I dati sono già nel formato corretto, semplicemente ritorna
        return payload;
    }

    /**
     * Imposta il timestamp REALE della simulazione nel payload estratto.
     * Legge il timestep da SimulationBridgeInterface (aggiornato dai Monitoring).
     */
    public void setTimestamp(TimeStepPayload payload, SimulationBridgeInterface simulationBridgeInterface) {
        if (payload != null && simulationBridgeInterface != null) {
            double realSimTime = simulationBridgeInterface.getCurrentSimTime();
            payload.setTimestamp(realSimTime);
        }
    }

    /**
     * Resetta i flag dirty dopo aver estratto i dati.
     * Chiamare SOLO per snapshot delta (fullSnapshot = false).
     */
    public void resetDirtyFlags(SimulationBridgeInterface simulationBridgeInterface) {
        if (simulationBridgeInterface != null) {
            simulationBridgeInterface.resetDirty();
        }
    }
}
