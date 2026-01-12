package org.springboot.controller;

import org.springboot.DTO.SimulationDTO.EvFleetDto;
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
@RequestMapping("/api/simulation")
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
    @PostMapping("/run")
    public ResponseEntity<String> runScenario(@Valid @RequestBody(required = false) SimulationSettingsDTO settings) {
        // Se il body è vuoto, usa un DTO nuovo (che ha i valori di default)
        SimulationSettingsDTO finalSettings = (settings != null) ? settings : new SimulationSettingsDTO();
        // Chiama il Service e usa la stringa di stato come risposta HTTP
        String status = matsimService.runThread(finalSettings);    
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
    @PostMapping("/shutdown")
    public ResponseEntity<String> shutdownScenario() {
        String status = matsimService.shutdownThread();
        if (status.contains("Tentativo di interruzione fallito")) {
            return ResponseEntity.status(503).body(status); // 503 Service Unavailable
        }
        return ResponseEntity.ok(status);
    }
    
    // --- Endpoint espone Veicoli della simulazione ----
    @Operation(summary = "Restituisce lo configurazione dei veicoli della simulazione")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Flotta di veicoli disponibile"),
            @ApiResponse(responseCode = "409", description = "Simulazione non avviata"),
            @ApiResponse(responseCode = "500", description = "Dati dei veicoli ancora non disponibili")
    })
    @GetMapping("/fleet")
    public ResponseEntity<EvFleetDto> getVehicles() {
        if (!matsimService.isSimulationRunning()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }

        EvFleetDto fleetDto = matsimService.getVehiclesInfo();
        if (fleetDto == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        return ResponseEntity.ok(fleetDto);
    }

    //--- Endpoint espone hub della simulazione ----
    @Operation(summary = "Restituisce lo configurazione degli hub della simulazione")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista hub disponibile"),
            @ApiResponse(responseCode = "409", description = "Simulazione non avviata"),
            @ApiResponse(responseCode = "500", description = "Dati hub non ancora disponibili")
    })
    @GetMapping("/hubs")
    public ResponseEntity<HubListDTO> getHubs() {
        if (!matsimService.isSimulationRunning()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }

        HubListDTO hubListDTO = matsimService.getHubsInfo();
        if (hubListDTO == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        return ResponseEntity.ok(hubListDTO);
    }

}
