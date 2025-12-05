package org.springboot.controller;

import java.util.Map;

import org.springboot.service.MatsimService;
import org.springboot.websocket.SimulationWebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "org.springboot")
@RestController
@RequestMapping("/api/simulation")
public class SimulationController {

    private final SimulationWebSocketService eventPublisher;

    @Autowired
    private MatsimService matsimService;

    public SimulationController(SimulationWebSocketService eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @PostMapping("/run")
    public ResponseEntity<String> runScenario() {

        new Thread() {
            public void run() {
                matsimService.runScenario();
            }
        }.start();

        return ResponseEntity.ok("Scenario MATSim avviato!");
    }

    
    @PostMapping("/test")
    public ResponseEntity<String> test() {
        eventPublisher.publish(Map.of("msg", "test connessione"));
        return ResponseEntity.ok("Scenario MATSim avviato!");
    }


}
