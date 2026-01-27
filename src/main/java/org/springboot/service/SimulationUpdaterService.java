package org.springboot.service;

import org.matsim.ServerEvSetup.SimulationInterface.SimulationBridgeInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springboot.service.result.ChargerStateUpdateResult;
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
    public ChargerStateUpdateResult setChargerState(SimulationBridgeInterface bridge, String chargerId, boolean isActive) {
        if (bridge == null) {
            return ChargerStateUpdateResult.SIMULATION_NOT_RUNNING;
        }

        try {
            bridge.updateChargerState(chargerId, isActive);
            return ChargerStateUpdateResult.SUCCESS;
        } catch (Exception e) {
            logger.error("Errore durante il cambio dello stato del caricatore", e);
            return ChargerStateUpdateResult.ERROR;
        }
    }



}
