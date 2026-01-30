package org.springboot.DTO.out.SimulationDTO.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springboot.DTO.out.SimulationDTO.ChargerDTO;
import org.springboot.DTO.out.SimulationDTO.HubDTO;
import org.springboot.service.generationService.DTO.ChargerSpecDto;
import org.springboot.service.generationService.DTO.HubSpecDto;

/**
 * Mapper per convertire HubSpecDto (modello puro lato server)
 * a HubDTO (DTO di output per API REST).
 * 
 * Supporta charger di tipo misto con potenze variabili.
 */
public class HubSpecMapper {

    /**
     * Converte HubSpecDto → HubDTO con supporto a charger di tipo misto.
     */
    public static HubDTO toHubDTO(HubSpecDto hubSpec) {
        if (hubSpec == null) {
            return null;
        }

        List<ChargerDTO> chargerDtos = hubSpec.getChargers()
                .stream()
                .map(HubSpecMapper::toChargerDTO)
                .collect(Collectors.toList());

        return new HubDTO(
            hubSpec.getHubId(),
            hubSpec.getLinkId(),
            chargerDtos
        );
    }

    /**
     * Converte ChargerSpecDto → ChargerDTO con tipo di charger e potenza.
     */
    public static ChargerDTO toChargerDTO(ChargerSpecDto chargerSpec) {
        if (chargerSpec == null) {
            return null;
        }

        return new ChargerDTO(
            chargerSpec.getChargerId(),
            chargerSpec.getChargerType(),
            chargerSpec.getPlugPowerKw()
        );
    }
}
