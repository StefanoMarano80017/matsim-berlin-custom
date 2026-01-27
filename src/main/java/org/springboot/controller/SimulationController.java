package org.springboot.controller;

import org.springboot.DTO.in.GenerationRequestDTO;
import org.springboot.DTO.in.SimulationSettingsDTO;
import org.springboot.DTO.out.SimulationResponseDTO;
import org.springboot.DTO.out.SimulationDTO.ChargerStateDTO;
import org.springboot.DTO.out.SimulationDTO.EvFleetDto;
import org.springboot.DTO.out.SimulationDTO.HubListDTO;
import org.springboot.service.MatsimService;
import org.springboot.service.result.ChargerStateUpdateResult;
import org.springboot.service.result.GenerationResult;
import org.springboot.service.result.SimulationStartResult;
import org.springboot.service.result.SimulationStopResult;
import org.springboot.service.simulationState.SimulationState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class SimulationController {

    private final MatsimService matsimService;

    @Autowired
    public SimulationController(MatsimService matsimService) {
        this.matsimService = matsimService;
    }

    // ---------------- Helper per ResponseEntity ----------------
    private <T> ResponseEntity<SimulationResponseDTO<T>> buildResponse(
        HttpStatus status, 
        String message, 
        T data, 
        String error
    ) {
        SimulationResponseDTO<T> response;
        if (status.is2xxSuccessful()) {
            response = SimulationResponseDTO.success(message, data);
        } else {
            response = SimulationResponseDTO.error(message, error);
        }
        return ResponseEntity.status(status).body(response);
    }

    // Sovraccarico senza dati aggiuntivi
    private <T> ResponseEntity<SimulationResponseDTO<T>> buildResponse(
        HttpStatus status, 
        String message
    ) {
        return buildResponse(status, message, null, null);
    }


    // --- Endpoint di Avvio ---
    @Operation(summary = "Esegue la simulazione con parametri opzionali")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Simulazione avviata con successo"),
            @ApiResponse(responseCode = "409", description = "Simulazione già in esecuzione"),
            @ApiResponse(responseCode = "400", description = "Flotta o hub non generati"),
            @ApiResponse(responseCode = "500", description = "Errore interno del server")
    })
    @PostMapping("/simulation/run")
    public ResponseEntity<SimulationResponseDTO<Void>> runScenario(@Valid @RequestBody(required = false) SimulationSettingsDTO settings) {
        SimulationSettingsDTO finalSettings = (settings != null) ? settings : new SimulationSettingsDTO();
        // Avvia la simulazione
        SimulationStartResult result = matsimService.runSimulationAsync(finalSettings);
        switch (result) {
            case SUCCESS:
                return buildResponse(HttpStatus.OK, result.getMessage());
            case FLEET_NOT_GENERATED, HUBS_NOT_GENERATED, FLEET_AND_HUBS_NOT_GENERATED:
                return buildResponse(HttpStatus.BAD_REQUEST, result.getMessage());
            case ALREADY_RUNNING:
                return buildResponse(HttpStatus.CONFLICT, SimulationStartResult.ALREADY_RUNNING.getMessage());
            case ERROR:
                return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, result.getMessage());
            default:
                return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Stato sconosciuto");
        }
    }

    // --- Endpoint di Arresto ---
    @Operation(summary = "Arresta la simulazione")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Richiesta di shutdown inviata con successo"),
            @ApiResponse(responseCode = "503", description = "Simulazione non in esecuzione"),
            @ApiResponse(responseCode = "500", description = "Errore durante l'arresto")
    })
    @PostMapping("/simulation/shutdown")
    public ResponseEntity<SimulationResponseDTO<Void>> shutdownScenario() {
        SimulationStopResult result = matsimService.stopSimulation();        
        switch (result) {
            case SUCCESS:
                SimulationResponseDTO<Void> response = SimulationResponseDTO.success(
                    result.getMessage()
                );
                /*
                *  Richiesta mandata correttamente, ma eccezione durante lo shutdown
                */
                Exception lastException = matsimService.getSimulationException();
                if (lastException != null) {
                    response.setError(lastException.getMessage());
                }

                return ResponseEntity.ok(response);
                
            case NOT_RUNNING:
                return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, result.getMessage(), null, "Stato attuale: " + matsimService.getSimulationState());
                
            case ERROR:
                return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, result.getMessage());

            default:  
                return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Stato sconosciuto");
        }
    }

    // --- Endpoint per controllare lo stato ---
    @Operation(summary = "Restituisce lo stato attuale della simulazione")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stato della simulazione")
    })
    @GetMapping("/simulation/status")
    public ResponseEntity<SimulationResponseDTO<String>> getSimulationStatus() {
        SimulationState state   = matsimService.getSimulationState();
        Exception lastException = matsimService.getSimulationException();
        String ExceptionString  = lastException != null ? lastException.getMessage() : null;
        return buildResponse(HttpStatus.OK, state.name(), null, ExceptionString);
    }

    /**
     * Genera i modelli EV lato server.
     * Legge dal CSV configurato e crea EvModel puri.
     * Può essere chiamato più volte per rigenerare.
     */
    @Operation(summary = "Genera la flotta EV lato server")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Flotta generata con successo"),
            @ApiResponse(responseCode = "400", description = "Richiesta invalida"),
            @ApiResponse(responseCode = "500", description = "Errore nella generazione")
    })
    @PostMapping("/fleet")
    public ResponseEntity<SimulationResponseDTO<EvFleetDto>> generateFleet(@Valid @RequestBody(required = false) GenerationRequestDTO request) {
        GenerationRequestDTO finalRequest = (request != null) ? request : new GenerationRequestDTO();
        GenerationResult result = matsimService.generateFleet(
            finalRequest.getCsvResourceEv(),
            finalRequest.getNumeroVeicoli(),
            finalRequest.getSocMedio(),
            finalRequest.getSocStdDev()
        );
        
        switch (result) {
            case SUCCESS:
                EvFleetDto fleet = matsimService.getGeneratedFleet();
                return buildResponse(HttpStatus.CREATED, result.getMessage(), fleet, null);
            case INVALID_REQUEST:
                return buildResponse(HttpStatus.BAD_REQUEST, result.getMessage());
            case ERROR:
                return buildResponse(HttpStatus.BAD_REQUEST, result.getMessage());
            default:
                return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Stato sconosciuto");
        }
    }

    /**
     * Recupera i modelli EV già generati lato server.
     * Non richiede che la simulazione sia in esecuzione.
     */
    @Operation(summary = "Restituisce i modelli EV generati lato server")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Flotta generata disponibile"),
            @ApiResponse(responseCode = "204", description = "Nessuna flotta generata"),
            @ApiResponse(responseCode = "500", description = "Errore nel recupero dati")
    })
    @GetMapping("/fleet")
    public ResponseEntity<SimulationResponseDTO<EvFleetDto>> getGeneratedFleet() {
        EvFleetDto fleetDto = matsimService.getGeneratedFleet();
        if (fleetDto == null || fleetDto.getVehicles() == null || fleetDto.getVehicles().isEmpty()) {
            return buildResponse(HttpStatus.NO_CONTENT, "Nessuna flotta generata", null, null);
        }
        return buildResponse(HttpStatus.OK, "CREATED", fleetDto, null);
    }

    /**
     * Genera i modelli degli hub lato server.
     * Legge dal CSV configurato e crea HubSpecDto puri.
     * Può essere chiamato più volte per rigenerare.
     */
    @Operation(summary = "Genera gli hub di ricarica lato server")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hub generati con successo"),
            @ApiResponse(responseCode = "400", description = "Richiesta invalida"),
            @ApiResponse(responseCode = "500", description = "Errore nella generazione")
    })
    @PostMapping("/hub")
    public ResponseEntity<SimulationResponseDTO<HubListDTO>> generateHubs(@Valid @RequestBody(required = false) GenerationRequestDTO request) {
        GenerationRequestDTO finalRequest = (request != null) ? request : new GenerationRequestDTO();        
        GenerationResult result = matsimService.generateHubs(finalRequest.getCsvResourceHub());
        switch (result) {
            case SUCCESS:
                HubListDTO hubs = matsimService.getGeneratedHubs();
                return buildResponse(HttpStatus.OK, result.getMessage(), hubs, null);
            case INVALID_REQUEST:
                return buildResponse(HttpStatus.BAD_REQUEST, result.getMessage());
            case ERROR:
                return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, result.getMessage());
            default:
                return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Stato sconosciuto");
        }
    }

    /**
     * Recupera i modelli degli hub già generati lato server.
     * Non richiede che la simulazione sia in esecuzione.
     */
    @Operation(summary = "Restituisce i modelli degli hub generati lato server")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hub generati disponibili"),
            @ApiResponse(responseCode = "204", description = "Nessun hub generato"),
            @ApiResponse(responseCode = "500", description = "Errore nel recupero dati")
    })
    @GetMapping("/hub")
    public ResponseEntity<SimulationResponseDTO<HubListDTO>> getGeneratedHubs() {
        HubListDTO hubListDTO = matsimService.getGeneratedHubs();
        if (hubListDTO == null || hubListDTO.getHubs() == null || hubListDTO.getHubs().isEmpty()) {
            return buildResponse(HttpStatus.NO_CONTENT, "Nessuna hub generata", null, null);
        }
        return buildResponse(HttpStatus.CREATED, "CREATED", hubListDTO, null);
    }

    @Operation(summary = "Aggiorna lo stato di una colonnina di un hub durante la simulazione")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Colonnina aggiornata"),
            @ApiResponse(responseCode = "400", description = "ID colonnina non valido"),
            @ApiResponse(responseCode = "503", description = "Simulazione non in esecuzione"),
            @ApiResponse(responseCode = "500", description = "Errore nell'aggiornamento")
    })
    @PostMapping("/ChargerState")
    public ResponseEntity<SimulationResponseDTO<ChargerStateUpdateResult>> setChargerState(@Valid @RequestBody ChargerStateDTO chargerStateDTO){
        ChargerStateUpdateResult result = matsimService.updateChargerState(
            chargerStateDTO.getChargerId(), 
            chargerStateDTO.getIsActive()
        );
        
        switch (result) {
            case SUCCESS:
                return buildResponse(HttpStatus.OK, result.getMessage());
            case SIMULATION_NOT_RUNNING:
                return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, result.getMessage());
            case INVALID_CHARGER_ID:
                return buildResponse(HttpStatus.BAD_REQUEST, result.getMessage());
            case ERROR:
                return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, result.getMessage());
            default:
                return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "errore sconosciuto");
        }
    }

}