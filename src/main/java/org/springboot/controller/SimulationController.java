package org.springboot.controller;

import org.springboot.DTO.SimulationDTO.EvFleetDto;
import org.springboot.DTO.SimulationDTO.GenerationRequestDTO;
import org.springboot.DTO.SimulationDTO.HubListDTO;
import org.springboot.DTO.SimulationDTO.SimulationSettingsDTO;
import org.springboot.service.MatsimService;
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
    })
    @PostMapping("/simulation/run")
    public ResponseEntity<String> runScenario(@Valid @RequestBody(required = false) SimulationSettingsDTO settings) {
        // Se il body è vuoto, usa un DTO nuovo (che ha i valori di default)
        SimulationSettingsDTO finalSettings = (settings != null) ? settings : new SimulationSettingsDTO();
        // Chiama il Service e usa la stringa di stato come risposta HTTP
        String status = matsimService.runSimulationAsync(finalSettings);    
        if (status.contains("già in esecuzione")) {
            return ResponseEntity.status(409).body(status); // 409 Conflict
        }
        return ResponseEntity.ok(status);
    }

    // --- Endpoint di Arresto ---
    @Operation(summary = "Arresta la simulazione")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Richiesta di shutdown inviata con successo, operazione asincrona"),
            @ApiResponse(responseCode = "503", description = "Simulazione non in esecuzione"),
    })
    @PostMapping("/shutdown/simulation")
    public ResponseEntity<String> shutdownScenario() {
        String status = matsimService.stopSimulation();
        if (status.contains("Tentativo di interruzione fallito")) {
            return ResponseEntity.status(503).body(status); // 503 Service Unavailable
        }
        return ResponseEntity.ok(status);
    }

    // ===============================================
    // ======= Generation API   ========
    // ===============================================

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

}