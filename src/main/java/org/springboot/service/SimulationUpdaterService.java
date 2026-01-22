package org.springboot.service;

import org.matsim.ServerEvSetup.SimulationInterface.SimulationBridgeInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/*
*  Inietta dati dinamicamente nella simulazione
*/
@Service
public class SimulationUpdaterService {
 
    private static final Logger logger = LoggerFactory.getLogger(SimulationUpdaterService.class);

    /*
    *   UPDATE STATE APIs
    */
    public String setChargerState(SimulationBridgeInterface bridge, String chargerId, boolean isActive) {
        if (bridge == null) {
            return "Nessuna simulazione in esecuzione.";
        }

        try {
            bridge.updateChargerState(chargerId, isActive);
            return "Stato aggiornato per charger" + chargerId;
        } catch (Exception e) {
            logger.error("Errore durante il cambio dello stato del caricatore", e);
            return "Errore: " + e.getMessage();
        }
    }



}
