package org.springboot.controller;

import org.springboot.DTO.SimulationDTO.ChargerStateDTO;
import org.springboot.DTO.SimulationDTO.EvFleetDto;
import org.springboot.DTO.SimulationDTO.GenerationRequestDTO;
import org.springboot.DTO.SimulationDTO.HubListDTO;
import org.springboot.DTO.SimulationDTO.SimulationResponseDTO;
import org.springboot.DTO.SimulationDTO.SimulationSettingsDTO;
import org.springboot.service.MatsimService;
import org.springboot.service.SimulationState.SimulationState;
import org.springboot.service.result.ChargerStateUpdateResult;
import org.springboot.service.result.GenerationResult;
import org.springboot.service.result.SimulationStartResult;
import org.springboot.service.result.SimulationStopResult;
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

    // --- Endpoint di Avvio ---
    @Operation(summary = "Esegue la simulazione con parametri opzionali")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Simulazione avviata con successo"),
            @ApiResponse(responseCode = "409", description = "Simulazione già in esecuzione"),
            @ApiResponse(responseCode = "400", description = "Flotta o hub non generati"),
            @ApiResponse(responseCode = "500", description = "Errore interno del server")
    })
    @PostMapping("/simulation/run")
    public ResponseEntity<SimulationResponseDTO> runScenario(@Valid @RequestBody(required = false) SimulationSettingsDTO settings) {
        SimulationSettingsDTO finalSettings = (settings != null) ? settings : new SimulationSettingsDTO();
        // Avvia la simulazione
        SimulationStartResult result = matsimService.runSimulationAsync(finalSettings);
        switch (result) {
            case SUCCESS:
                SimulationResponseDTO response = SimulationResponseDTO.success(
                    SimulationState.RUNNING.name(),
                    result.getMessage()
                );
                return ResponseEntity.ok(response);
            case FLEET_NOT_GENERATED:
                SimulationResponseDTO NoFleetResponse = SimulationResponseDTO.error(
                    SimulationState.ERROR.name(),
                    result.getMessage(),
                    null
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(NoFleetResponse);
            case HUBS_NOT_GENERATED:
                SimulationResponseDTO NoHubResponse = SimulationResponseDTO.error(
                    SimulationState.ERROR.name(),
                    result.getMessage(),
                    null
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(NoHubResponse);
            case FLEET_AND_HUBS_NOT_GENERATED:
                SimulationResponseDTO NoHubNoFleetResponse = SimulationResponseDTO.error(
                    SimulationState.ERROR.name(),
                    result.getMessage(),
                    null
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(NoHubNoFleetResponse);
            case ALREADY_RUNNING:
                SimulationResponseDTO AlreadyResponse = SimulationResponseDTO.error(
                    SimulationState.RUNNING.name(),
                    SimulationStartResult.ALREADY_RUNNING.getMessage(),
                    null
                );
                return ResponseEntity.status(HttpStatus.CONFLICT).body(AlreadyResponse);
            case ERROR:
                SimulationResponseDTO errorResponse2 = SimulationResponseDTO.error(
                    SimulationState.ERROR.name(),
                    result.getMessage(),
                    null
                );
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse2);
            default:
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SimulationResponseDTO.error(SimulationState.ERROR.name(), "Stato sconosciuto", null));
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
    public ResponseEntity<SimulationResponseDTO> shutdownScenario() {
        SimulationStopResult result = matsimService.stopSimulation();        
        switch (result) {
            case SUCCESS:
                SimulationResponseDTO response = SimulationResponseDTO.success(
                    "SUCCESS",
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
                SimulationResponseDTO NoRunningResponse = SimulationResponseDTO.error(
                    "NOT_ALLOWED",
                    result.getMessage(),
                    "Stato attuale: " + matsimService.getSimulationState()
                );
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(NoRunningResponse);
                
            case ERROR:
                SimulationResponseDTO errorResponse = SimulationResponseDTO.error(
                    SimulationState.ERROR.name(),
                    result.getMessage(),
                    null
                );
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                
            default:
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SimulationResponseDTO.error(SimulationState.ERROR.name(), "Stato sconosciuto", null));
        }
    }

    // --- Endpoint per controllare lo stato ---
    @Operation(summary = "Restituisce lo stato attuale della simulazione")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stato della simulazione")
    })
    @GetMapping("/simulation/status")
    public ResponseEntity<SimulationResponseDTO> getSimulationStatus() {
        SimulationState state = matsimService.getSimulationState();
        Exception lastException = matsimService.getSimulationException();
        SimulationResponseDTO response = new SimulationResponseDTO(
            state.name(),
            "Stato attuale: " + state.getDescription(),
            lastException != null ? lastException.getMessage() : null
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Genera i modelli EV lato server.
     * Legge dal CSV configurato e crea EvModel puri.
     * Può essere chiamato più volte per rigenerare.
     */
    @Operation(summary = "Genera la flotta EV lato server")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Flotta generata con successo"),
            @ApiResponse(responseCode = "400", description = "Richiesta invalida"),
            @ApiResponse(responseCode = "500", description = "Errore nella generazione")
    })
    @PostMapping("/fleet")
    public ResponseEntity<?> generateFleet(@Valid @RequestBody(required = false) GenerationRequestDTO request) {
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
                return ResponseEntity.ok(fleet);
            case INVALID_REQUEST:
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(SimulationResponseDTO.error("INVALID_REQUEST", result.getMessage(), null));           
            case ERROR:
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SimulationResponseDTO.error("Exception ERROR", result.getMessage(), null));
            default:
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SimulationResponseDTO.error("ERROR", "Stato sconosciuto", null));
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
    public ResponseEntity<EvFleetDto> getGeneratedFleet() {
        EvFleetDto fleetDto = matsimService.getGeneratedFleet();
        if (fleetDto == null || fleetDto.getVehicles() == null || fleetDto.getVehicles().isEmpty()) {
            return ResponseEntity.noContent().build(); // 204 No Content
        }
        return ResponseEntity.ok(fleetDto);
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
    public ResponseEntity<?> generateHubs(@Valid @RequestBody(required = false) GenerationRequestDTO request) {
        GenerationRequestDTO finalRequest = (request != null) ? request : new GenerationRequestDTO();        
        GenerationResult result = matsimService.generateHubs(finalRequest.getCsvResourceHub());
        switch (result) {
            case SUCCESS:
                HubListDTO hubs = matsimService.getGeneratedHubs();
                return ResponseEntity.ok(hubs);
                
            case INVALID_REQUEST:
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(SimulationResponseDTO.error("INVALID_REQUEST", result.getMessage(), null));
            case ERROR:
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SimulationResponseDTO.error("Exception ERROR", result.getMessage(), null));
            default:
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SimulationResponseDTO.error("ERROR", "Stato sconosciuto", null));
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
    public ResponseEntity<HubListDTO> getGeneratedHubs() {
        HubListDTO hubListDTO = matsimService.getGeneratedHubs();
        if (hubListDTO == null || hubListDTO.getHubs() == null || hubListDTO.getHubs().isEmpty()) {
            return ResponseEntity.noContent().build(); // 204 No Content
        }
        return ResponseEntity.ok(hubListDTO);
    }

    @Operation(summary = "Aggiorna lo stato di una colonnina di un hub durante la simulazione")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Colonnina aggiornata"),
            @ApiResponse(responseCode = "400", description = "ID colonnina non valido"),
            @ApiResponse(responseCode = "503", description = "Simulazione non in esecuzione"),
            @ApiResponse(responseCode = "500", description = "Errore nell'aggiornamento")
    })
    @PostMapping("/ChargerState")
    public ResponseEntity<SimulationResponseDTO> setChargerState(@Valid @RequestBody ChargerStateDTO chargerStateDTO){
        ChargerStateUpdateResult result = matsimService.updateChargerState(
            chargerStateDTO.getChargerId(), 
            chargerStateDTO.getIsActive()
        );
        
        switch (result) {
            case SUCCESS:
                SimulationResponseDTO response = SimulationResponseDTO.success(
                    "OK",
                    result.getMessage()
                );
                return ResponseEntity.ok(response);
                
            case SIMULATION_NOT_RUNNING:
                SimulationResponseDTO notRunningResponse = SimulationResponseDTO.error(
                    "SIMULATION_NOT_RUNNING",
                    result.getMessage(),
                    null
                );
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(notRunningResponse);
                
            case INVALID_CHARGER_ID:
                SimulationResponseDTO invalidResponse = SimulationResponseDTO.error(
                    "INVALID_REQUEST",
                    result.getMessage(),
                    null
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(invalidResponse);
                
            case ERROR:
                SimulationResponseDTO errorResponse = SimulationResponseDTO.error(
                    "ERROR",
                    result.getMessage(),
                    null
                );
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
                
            default:
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SimulationResponseDTO.error(SimulationState.ERROR.name(), "Stato sconosciuto", null));
        }
    }

}