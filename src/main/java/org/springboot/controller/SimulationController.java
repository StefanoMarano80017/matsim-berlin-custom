package org.springboot.controller;

import org.springboot.DTO.SimulationDTO.ChargerStateDTO;
import org.springboot.DTO.SimulationDTO.EvFleetDto;
import org.springboot.DTO.SimulationDTO.GenerationRequestDTO;
import org.springboot.DTO.SimulationDTO.HubListDTO;
import org.springboot.DTO.SimulationDTO.SimulationSettingsDTO;
import org.springboot.DTO.SimulationDTO.SimulationResponseDTO;
import org.springboot.service.MatsimService;
import org.springboot.service.SimulationState.SimulationState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "org.springboot")
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
        try {
            // Se il body è vuoto, usa un DTO nuovo (che ha i valori di default)
            SimulationSettingsDTO finalSettings = (settings != null) ? settings : new SimulationSettingsDTO();
            
            // Controlla se la simulazione è già in esecuzione
            if (matsimService.isSimulationRunning()) {
                SimulationResponseDTO response = SimulationResponseDTO.error(
                    SimulationState.RUNNING.name(),
                    "Simulazione già in esecuzione",
                    null
                );
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            // Avvia la simulazione
            String message = matsimService.runSimulationAsync(finalSettings);
            
            // Controlla se ci sono stati errori
            if (message.contains("Errore")) {
                SimulationResponseDTO response = SimulationResponseDTO.error(
                    SimulationState.ERROR.name(),
                    message,
                    null
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Simulazione avviata con successo
            SimulationResponseDTO response = SimulationResponseDTO.success(
                SimulationState.RUNNING.name(),
                "Simulazione avviata con successo"
            );
            log.info("[SimulationAPI] Simulazione avviata");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("[SimulationAPI] Errore nell'avvio della simulazione", e);
            SimulationResponseDTO response = SimulationResponseDTO.error(
                SimulationState.ERROR.name(),
                "Errore nell'avvio della simulazione",
                e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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
        try {
            // Controlla se la simulazione è in esecuzione
            SimulationState currentState = matsimService.getSimulationState();
            
            if (currentState != SimulationState.RUNNING) {
                SimulationResponseDTO response = SimulationResponseDTO.error(
                    currentState.name(),
                    "Nessuna simulazione attiva",
                    "Stato attuale: " + currentState.getDescription()
                );
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }

            // Arresta la simulazione
            String message = matsimService.stopSimulation();
            
            // Verifica se c'è stata un'eccezione durante l'arresto
            Exception lastException = matsimService.getSimulationException();
            SimulationResponseDTO response = SimulationResponseDTO.success(
                SimulationState.STOPPED.name(),
                message
            );
            
            if (lastException != null) {
                response.setError(lastException.getMessage());
                log.warn("[SimulationAPI] Errore durante l'arresto: {}", lastException.getMessage());
            }

            log.info("[SimulationAPI] Richiesta di interruzione inviata");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("[SimulationAPI] Errore nell'arresto della simulazione", e);
            SimulationResponseDTO response = SimulationResponseDTO.error(
                SimulationState.ERROR.name(),
                "Errore nell'arresto della simulazione",
                e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // --- Endpoint per controllare lo stato ---
    @Operation(summary = "Restituisce lo stato attuale della simulazione")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stato della simulazione")
    })
    @GetMapping("/simulation/status")
    public ResponseEntity<SimulationResponseDTO> getSimulationStatus() {
        try {
            SimulationState state = matsimService.getSimulationState();
            Exception lastException = matsimService.getSimulationException();
            
            SimulationResponseDTO response = new SimulationResponseDTO(
                state.name(),
                "Stato attuale: " + state.getDescription(),
                lastException != null ? lastException.getMessage() : null
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[SimulationAPI] Errore nel recupero dello stato", e);
            SimulationResponseDTO response = SimulationResponseDTO.error(
                SimulationState.ERROR.name(),
                "Errore nel recupero dello stato",
                e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
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
        try {
            // Se il body è vuoto, usa un DTO nuovo (che ha i valori di default)
            GenerationRequestDTO finalRequest = (request != null) ? request : new GenerationRequestDTO();
            String message = matsimService.generateFleet(
                finalRequest.getCsvResourceEv(),
                finalRequest.getNumeroVeicoli(),
                finalRequest.getSocMedio(),
                finalRequest.getSocStdDev()
            );
            log.info("[GenerationAPI] Fleet generated: {}", message);
            return ResponseEntity.ok(matsimService.getGeneratedFleet());
        } catch (Exception e) {
            log.error("[GenerationAPI] Errore nella generazione della flotta", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Errore: " + e.getMessage());
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
        try {
            // Se il body è vuoto, usa un DTO nuovo (che ha i valori di default)
            GenerationRequestDTO finalRequest = (request != null) ? request : new GenerationRequestDTO();
            String message = matsimService.generateHubs(finalRequest.getCsvResourceHub());
            log.info("[GenerationAPI] Hubs generated: {}", message);
            return ResponseEntity.ok(matsimService.getGeneratedHubs());
        } catch (Exception e) {
            log.error("[GenerationAPI] Errore nella generazione degli hub", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Errore: " + e.getMessage());
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
    })
    @PostMapping("/ChargerState")
    public ResponseEntity<String> setChargerState(@Valid @RequestBody ChargerStateDTO chargerStateDTO){
        String status = matsimService.updateChargerState(chargerStateDTO.getChargerId(), chargerStateDTO.getIsActive());
        return ResponseEntity.ok(status);
    }

}