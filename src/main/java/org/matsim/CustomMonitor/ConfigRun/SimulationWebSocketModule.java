package org.matsim.CustomMonitor.ConfigRun;

import org.matsim.core.controler.AbstractModule;
import org.springboot.websocket.SimulationBridge;
import org.springframework.context.ApplicationContext; // Avrai bisogno del contesto Spring

public class SimulationWebSocketModule extends AbstractModule {
    
    private final ApplicationContext applicationContext;

    // Passiamo il contesto Spring al modulo Guice
    public SimulationWebSocketModule(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void install() {
        if (applicationContext == null) {
            // Se questo accade, significa che l'impostazione statica è fallita.
            throw new IllegalStateException("Impossibile installare SimulationWebSocketModule: ApplicationContext è NULL.");
        }
        // 1. Ottieni l'istanza del componente Spring (SimulationBridge)
        final SimulationBridge bridgeInstance = applicationContext.getBean(SimulationBridge.class);
        // 2. Lega l'interfaccia al bean di Spring!
        // Quando una classe MATSim chiede "SimulationEventPublisher", Guice le darà
        // l'istanza del bridge che hai recuperato da Spring.
        bind(SimulationBridge.class).toInstance(bridgeInstance);
    }
}