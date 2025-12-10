package org.springboot.controller;

import org.springboot.service.MatsimService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;

import org.springboot.websocket.*;

@Slf4j(topic = "org.springboot")
@RestController
@RequestMapping("/api/simulation")
public class SimulationController {

    private final SimulationBridge simulationBridge;
    private final MatsimService matsimService;

    @Autowired
    public SimulationController(SimulationBridge simulationBridge, MatsimService matsimService) {
        this.simulationBridge = simulationBridge;
        this.matsimService = matsimService;
    }

    // --- Endpoint di Avvio ---
    @PostMapping("/run")
    public ResponseEntity<String> runScenario() {
        // Chiama il Service e usa la stringa di stato come risposta HTTP
        String status = matsimService.runThread();    
        if (status.contains("già in esecuzione")) {
            return ResponseEntity.status(409).body(status); // 409 Conflict
        }
        return ResponseEntity.ok(status);
    }

    // --- Endpoint di Arresto ---
    @PostMapping("/shutdown")
    public ResponseEntity<String> shutdownScenario() {
        String status = matsimService.shutdownThread();
        if (status.contains("Tentativo di interruzione fallito")) {
            return ResponseEntity.status(503).body(status); // 503 Service Unavailable
        }
        return ResponseEntity.ok(status);
    }
    
    @PostMapping("/test")
    public ResponseEntity<String> test() {
        simulationBridge.publishSimpleText("Test connessione");
        return ResponseEntity.ok("Scenario MATSim avviato!");
    }
}
