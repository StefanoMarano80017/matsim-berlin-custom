package org.springboot.DTO.out.SimulationDTO;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class ChargerStateDTO {

    @NotBlank(message = "ChargerId è obbligatoria")
    private String chargerId;

    @NotNull(message = "isActive è obbligatorio")
    private Boolean isActive;
}
