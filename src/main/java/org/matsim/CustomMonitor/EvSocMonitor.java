package org.matsim.CustomMonitor;

import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.qsim.QSim;
import com.google.inject.Inject;

/**
 * Monitora periodicamente lo Stato di Carica (SOC) e il livello di energia
 * della flotta EV prima di ogni step della simulazione (MobsimBeforeSimStepEvent).
 */
public class EvSocMonitor implements MobsimBeforeSimStepListener {
    private final EvFleetManager fleetManager; // Il gestore della flotta
    private final double interval = 300.0; // 5 Minuti (in secondi)
    private double lastCheckTime = -300.0;
    /**
     * Il gestore della flotta viene iniettato automaticamente da Guice.
     */
    @Inject
    public EvSocMonitor(EvFleetManager fleetManager) {
        this.fleetManager = fleetManager;
    }
    /**
     * Helper per formattare il tempo come H:M:S
     */
    private String formatTime(double seconds) {
        int h = (int) (seconds / 3600);
        int m = (int) ((seconds % 3600) / 60);
        int s = (int) (seconds % 60);
        return String.format("%02d:%02d:%02d", h, m, s);
    }
    // --- Monitoraggio Periodico veicoli ---
    @Override
    public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
        double now = e.getSimulationTime();
        // Controlla se è il momento di registrare i dati 
        if (now >= lastCheckTime + interval) {
            QSim qSim = (QSim) e.getQueueSimulation();
            System.out.println("\n--- MONITORAGGIO SOC EV PERIODICO [T=" + formatTime(now) + "] ---");    
            // Aggiorno lo stato della flotta a partire dai dati della simulazione
            fleetManager.updateVehiclesFromQSim(qSim);
            // Esempio di analisi che puoi fare ora:
            System.out.format("Analisi Flotta: SOC Medio = %.2f%%\n", fleetManager.calculateAverageSoc() * 100);
            lastCheckTime = now;

        }
    }
}