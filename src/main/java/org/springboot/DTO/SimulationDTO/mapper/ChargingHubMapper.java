package org.springboot.DTO.SimulationDTO.mapper;

import org.matsim.CustomEvModule.Hub.ChargingHub;
import org.springboot.DTO.SimulationDTO.ChargerDTO;
import org.springboot.DTO.SimulationDTO.HubDTO;

import java.util.List;
import java.util.stream.Collectors;

public class ChargingHubMapper {

    /**
     * Funzione di mapping: ChargingHub -> HubDTO
     *
     * @param hub ChargingHub da mappare
     * @return HubDTO
     */
    public static HubDTO toHubDTO(ChargingHub hub) {
        List<ChargerDTO> chargerDTOs = hub.getChargers().stream()
                .map(chId -> new ChargerDTO(
                        chId.toString(),
                        hub.getPlugs(chId) // plug disponibili
                ))
                .collect(Collectors.toList());

        return new HubDTO(hub.getId(), hub.getLink().toString(),chargerDTOs);
    }
}
