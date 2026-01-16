package org.springboot.DTO.SimulationDTO.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springboot.DTO.SimulationDTO.ChargerDTO;
import org.springboot.DTO.SimulationDTO.HubDTO;
import org.springboot.service.GenerationService.DTO.ChargerSpecDto;
import org.springboot.service.GenerationService.DTO.HubSpecDto;

/**
 * Mapper per convertire HubSpecDto (modello puro lato server)
 * a HubDTO (DTO di output per API REST).
 */
public class HubSpecMapper {

    /**
     * Converte HubSpecDto → HubDTO
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
     * Converte ChargerSpecDto → ChargerDTO
     */
    public static ChargerDTO toChargerDTO(ChargerSpecDto chargerSpec) {
        if (chargerSpec == null) {
            return null;
        }

        return new ChargerDTO(
            chargerSpec.getChargerId(),
            null
        );
    }
}
