package org.springboot.DTO.SimulationDTO;

import java.util.Set;

public class ChargerDTO {

    private String chargerId;
    private Set<String> availablePlugs;

    public ChargerDTO(String chargerId, Set<String> availablePlugs) {
        this.chargerId = chargerId;
        this.availablePlugs = availablePlugs;
    }

    public String getChargerId() {
        return chargerId;
    }

    public void setChargerId(String chargerId) {
        this.chargerId = chargerId;
    }

    public Set<String> getAvailablePlugs() {
        return availablePlugs;
    }

    public void setAvailablePlugs(Set<String> availablePlugs) {
        this.availablePlugs = availablePlugs;
    }
}
