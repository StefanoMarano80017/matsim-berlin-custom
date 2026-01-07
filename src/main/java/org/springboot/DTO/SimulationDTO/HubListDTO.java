package org.springboot.DTO.SimulationDTO;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.matsim.CustomEvModule.Hub.ChargingHub;
import org.springboot.DTO.SimulationDTO.mapper.ChargingHubMapper;

public class HubListDTO {

    private List<HubDTO> hubs;

    public HubListDTO(List<HubDTO> hubs) {
        this.hubs = hubs;
    }

    public HubListDTO(Collection<ChargingHub> chargingHubs) {
        this.hubs = chargingHubs.stream().map(ChargingHubMapper::toHubDTO).collect(Collectors.toList());
    }

    public List<HubDTO> getHubs() {
        return hubs;
    }

    public void setHubs(List<HubDTO> hubs) {
        this.hubs = hubs;
    }
}
