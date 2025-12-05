package org.matsim.CustomMonitor.ChargingHub;

import org.matsim.CustomMonitor.EVfleet.EvFleetManager;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.charging.ChargingStartEvent;
import org.matsim.contrib.ev.charging.ChargingEndEvent;
import org.matsim.contrib.ev.charging.ChargingStartEventHandler;
import org.matsim.contrib.ev.charging.ChargingEndEventHandler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HubManager implements ChargingStartEventHandler, ChargingEndEventHandler {

    private static final Logger log = LogManager.getLogger(EvFleetManager.class);

    private final HubGenerator generator;
    private final ChargingInfrastructureSpecification infraSpec;

    // Mappa hub e charger
    private final Map<Id<Charger>, String> charger2hub = new HashMap<>();
    private final Map<Id<Charger>, Double> chargerEnergy = new HashMap<>();
    private final Map<String, Double> hubEnergy = new HashMap<>();
    private final Map<String, Integer> hubOccupancy = new HashMap<>();
    private final Map<Double, Map<String, Integer>> timeline = new HashMap<>();

    public HubManager(Network network, ChargingInfrastructureSpecification infraSpec) {
        this.infraSpec = infraSpec;
        this.generator = new HubGenerator(network, infraSpec);
    }

    // ------------------------
    // Creazione hub tramite generator
    // ------------------------
    public void createHub(
        Path csvFile
    ) {
        try {
            generator.generateHubsFromCSV(csvFile);
            log.debug("[HubManager] Hub e colonnine generate con successo.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        // registra tutti i charger già presenti nella infrastruttura
        this.infraSpec.getChargerSpecifications().values().forEach(chSpec -> {
            Id<Charger> chId = chSpec.getId();
            String hubId = (String) chSpec.getAttributes().getAttribute("hubId");
            if (hubId == null) throw new IllegalArgumentException("Charger " + chId + " senza hubId");

            charger2hub.put(chId, hubId);
            chargerEnergy.put(chId, 0.0);

            hubEnergy.putIfAbsent(hubId, 0.0);
            hubOccupancy.putIfAbsent(hubId, 0);
        });

        log.debug("[HubManager] Hub e colonnine create e registrate.");
    }

    public void registerChargers(List<Charger> chargers, String hubId) {
        for (Charger ch : chargers) {
            charger2hub.put(ch.getId(), hubId);
            chargerEnergy.put(ch.getId(), 0.0);
        }
        hubEnergy.putIfAbsent(hubId, 0.0);
        hubOccupancy.putIfAbsent(hubId, 0);
    }

    // ------------------------
    // Event handler
    // ------------------------
    @Override
    public void handleEvent(ChargingStartEvent event) {
        String hubId = charger2hub.get(event.getChargerId());
        hubOccupancy.put(hubId, hubOccupancy.get(hubId) + 1);
        recordTimeline(event.getTime());

        // --- Logging ---
        log.debug("[HubManager] [%.0f] START charging: charger=%s, hub=%s, occupancy=%d%n",
            event.getTime(),
            event.getChargerId(),
            hubId,
            hubOccupancy.get(hubId));
    }

    @Override
    public void handleEvent(ChargingEndEvent event) {
        String hubId = charger2hub.get(event.getChargerId());
        hubOccupancy.put(hubId, hubOccupancy.get(hubId) - 1);

        double energy = event.getCharge();
        chargerEnergy.put(event.getChargerId(), chargerEnergy.get(event.getChargerId()) + energy);
        hubEnergy.put(hubId, hubEnergy.get(hubId) + energy);

        recordTimeline(event.getTime());

        // --- Logging ---
        log.debug("[HubManager] [%.0f] END charging: charger=%s, hub=%s, charge=%.2fJ, occupancy=%d, totalHubEnergy=%.2fJ%n",
            event.getTime(),
            event.getChargerId(),
            hubId,
            energy,
            hubOccupancy.get(hubId),
            hubEnergy.get(hubId));
    }

    private void recordTimeline(double time) {
        Map<String, Integer> snapshot = new HashMap<>();
        hubOccupancy.forEach(snapshot::put);
        timeline.put(time, snapshot);
    }

    // ------------------------
    // Accessor e CSV (uguale a prima)
    // ------------------------
    public double getChargerEnergy(Id<Charger> chargerId) { return chargerEnergy.get(chargerId); }
    public double getHubEnergy(String hubId) { return hubEnergy.get(hubId); }
    public int getHubOccupancy(String hubId) { return hubOccupancy.get(hubId); }
    public Map<String, Double> getHubEnergyMap() { return hubEnergy; }
    public Map<String, Integer> getHubOccupancyMap() { return hubOccupancy; }
    public Map<Double, Map<String, Integer>> getTimeline() { return timeline; }

    public void resetAll() {
        chargerEnergy.replaceAll((k,v) -> 0.0);
        hubEnergy.replaceAll((k,v) -> 0.0);
        hubOccupancy.replaceAll((k,v) -> 0);
        timeline.clear();
    }
}