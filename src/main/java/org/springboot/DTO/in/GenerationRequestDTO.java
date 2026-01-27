package org.springboot.DTO.in;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO per le richieste di generazione lato server (fleet/hub).
 * 
 * Contiene:
 * - type: tipo di generazione (es. "csv")
 * - I parametri sono gestiti dal server
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerationRequestDTO {

    @NotBlank(message = "Il tipo di generazione è obbligatorio")
    private String type = "csv";
    
    @NotBlank(message = "La risorsa CSV per i veicoli è obbligatoria")
    String csvResourceEv = "csv/ev-dataset.csv";

    @NotBlank(message = "La risorsa CSV per gli hub è obbligatoria")
    String csvResourceHub = "csv/charging_hub.csv";

    @Min(value = 1, message = "Deve esserci almeno un veicolo")
    Integer numeroVeicoli = 2;

    @DecimalMin("0.0") @DecimalMax("1.0")
    Double socMedio = 0.70;

    @DecimalMin("0.0") @DecimalMax("1.0")
    Double socStdDev = 0.05;
}
