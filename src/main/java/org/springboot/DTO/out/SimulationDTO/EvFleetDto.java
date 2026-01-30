package org.springboot.DTO.out.SimulationDTO;

import java.util.List;

import org.matsim.CustomEvModule.EVfleet.EvModel;
import org.springboot.DTO.out.SimulationDTO.mapper.EvMapper;

public class EvFleetDto {

    private List<EvDto> vehicles;

    public EvFleetDto(List<EvModel> evModels) {
        this.vehicles = evModels.stream().map(EvMapper::toDto).toList();
    }

    public List<EvDto> getVehicles() {
        return vehicles;
    }

}
