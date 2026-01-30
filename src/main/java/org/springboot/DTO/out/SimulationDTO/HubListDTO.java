package org.springboot.DTO.out.SimulationDTO;

import java.util.List;

public class HubListDTO {

    private List<HubDTO> hubs;

    public HubListDTO(List<HubDTO> hubs) {
        this.hubs = hubs;
    }

    public List<HubDTO> getHubs() {
        return hubs;
    }

    public void setHubs(List<HubDTO> hubs) {
        this.hubs = hubs;
    }
}
