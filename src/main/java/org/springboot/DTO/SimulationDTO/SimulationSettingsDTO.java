package org.springboot.DTO.SimulationDTO;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.matsim.ServerEvSetup.ConfigRun.ConfigRun.VehicleGenerationStrategyEnum;
import org.matsim.ServerEvSetup.ConfigRun.ConfigRun.PlanGenerationStrategyEnum;

@Data
public class SimulationSettingsDTO {

    @NotBlank(message = "Il path della configurazione è obbligatorio")
    private String configPath = "input/v%s/berlin-v%s.config.xml";

    @NotBlank(message = "La risorsa CSV per gli hub è obbligatoria")
    private String csvResourceHub = "csv/charging_hub.csv";

    @NotBlank(message = "La risorsa CSV per i veicoli è obbligatoria")
    private String csvResourceEv = "csv/ev-dataset.csv";

    private VehicleGenerationStrategyEnum vehicleStrategy = VehicleGenerationStrategyEnum.FROM_CSV;
    private PlanGenerationStrategyEnum planStrategy = PlanGenerationStrategyEnum.STATIC;

    @DecimalMin(value = "0.0", inclusive = true)
    @DecimalMax(value = "1.0", inclusive = true)
    private Double sampleSizeStatic = 0.001;

    @Min(value = 1, message = "Deve esserci almeno un veicolo")
    private Integer numeroVeicoli = 2;
    
    // Validazione State of Charge (SoC) tra 0% e 100%
    @DecimalMin("0.0") @DecimalMax("1.0")
    private Double socMedio = 0.70;

    @PositiveOrZero
    private Double socStdDev = 0.05;

    @DecimalMin("0.0") @DecimalMax("1.0")
    private Double targetSocMean = 0.90;

    @PositiveOrZero
    private Double targetSocStdDev = 0.05;

    private Boolean debugLink = false;
    
    @Positive(message = "Lo stepSize deve essere maggiore di zero")
    private Double stepSize = 150.0; 

    //Setup publisher
    @Positive(message = "Il rate del publisher deve essere maggiore di zero")
    private Long publisherRateMs = 30000L; // default 30s
    private boolean publisherDirty = false; // default snapshot full

    private Boolean RealTime = false;
}