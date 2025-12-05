package org.springboot.service;

import java.util.Map;

import org.checkerframework.checker.units.qual.t;
import org.matsim.run.OpenBerlinScenario;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import org.springboot.websocket.SimulationEventPublisher;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j(topic = "org.springboot")
public class MatsimService {

    private final SimulationEventPublisher eventPublisher;

    public MatsimService(SimulationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void runScenario() {
        log.info("Preparazione scenario MATSim...");
        OpenBerlinScenario scenario = new OpenBerlinScenario(eventPublisher);

        log.info("Avvio simulazione MATSim...");
        scenario.RunSimulation();
        log.info("Scenario MATSim completato!");

        eventPublisher.publish("{\"event\":\"simulationComplete\"}");
    }
}