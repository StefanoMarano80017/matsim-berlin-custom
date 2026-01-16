package org.springboot.service.GenerationService.DTO;

import java.util.ArrayList;
import java.util.List;

/**
 * Modello di dominio lato server per un hub di ricarica.
 * Completamente indipendente da MATSim.
 * 
 * Contiene le informazioni essenziali di un hub necessarie per:
 * - trasportare tramite bridge
 * - tradurre in ChargingHub e specifiche MATSim quando registrato
 */
public class HubSpecDto {

    private final String hubId;
    private final String linkId;
    private final List<ChargerSpecDto> chargers;

    public HubSpecDto(String hubId, String linkId) {
        this.hubId = hubId;
        this.linkId = linkId;
        this.chargers = new ArrayList<>();
    }

    public String getHubId() {
        return hubId;
    }

    public String getLinkId() {
        return linkId;
    }

    public List<ChargerSpecDto> getChargers() {
        return chargers;
    }

    /**
     * Aggiunge un charger al hub.
     */
    public void addCharger(ChargerSpecDto charger) {
        chargers.add(charger);
    }

    @Override
    public String toString() {
        return "HubSpecDto{" +
                "hubId='" + hubId + '\'' +
                ", linkId='" + linkId + '\'' +
                ", chargers=" + chargers.size() +
                '}';
    }
}
