package org.springboot.controller;

import org.springboot.service.MatsimService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "org.springboot")
@RestController
@RequestMapping("/api/simulation")
public class SimulationController {

    private final MatsimService matsimService;

    // endpoint veicoli e piani  
    // modificare il DTO colonnine per contenere id veicolo 

    @Autowired
    public SimulationController(MatsimService matsimService) {
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
        //simulationBridge.publishWsSimpleText("Test connessione");
        return ResponseEntity.ok("Scenario MATSim avviato!");
    }
}
