package org.springboot.DTO.SimulationDTO;

import java.util.List;

public class HubDTO {

    private String hubId;
    private String linkid;
    private List<ChargerDTO> chargers;

    public HubDTO(String hubId, String linkid, List<ChargerDTO> chargers) {
        this.hubId = hubId;
        this.linkid = linkid;
        this.chargers = chargers;
    }

    public String getHubId() {
        return hubId;
    }

    public void setHubId(String hubId) {
        this.hubId = hubId;
    }

    public List<ChargerDTO> getChargers() {
        return chargers;
    }

    public void setChargers(List<ChargerDTO> chargers) {
        this.chargers = chargers;
    }

    public String getLinkid() {
        return linkid;
    }

    public void setLinkid(String linkid) {
        this.linkid = linkid;
    }
}
