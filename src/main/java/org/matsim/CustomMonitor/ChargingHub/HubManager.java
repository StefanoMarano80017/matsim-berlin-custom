package org.matsim.CustomMonitor.ChargingHub;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Gestione hub di ricarica con dirty flags per ottimizzare aggiornamenti WebSocket.
 */
public class HubManager {

    private static final Logger log = LogManager.getLogger(HubManager.class);

    private final HubGenerator generator;
    private final ChargingInfrastructureSpecification infraSpec;

    private final Map<Id<Charger>, String> charger2hub       = new HashMap<>();
    private final Map<Id<Charger>, Double> chargerEnergy     = new HashMap<>();
    private final Map<String, Double> hubEnergy              = new HashMap<>();
    private final Map<String, Integer> hubOccupancy          = new HashMap<>();
    private final Map<Double, Map<String, Integer>> timeline = new HashMap<>();

    // ------------------------
    // Dirty flags
    // ------------------------
    private final Map<String, Boolean> hubDirtyFlags = new HashMap<>();

    public HubManager(Network network, ChargingInfrastructureSpecification infraSpec) {
        this.infraSpec = infraSpec;
        this.generator = new HubGenerator(network, infraSpec);
    }

    public void createHub(Resource csvFile) {
        try {
            generator.generateHubsFromCSV(csvFile);
            log.debug("[HubManager] Hub e colonnine generate con successo.");
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.infraSpec.getChargerSpecifications().values().forEach(chSpec -> {
            Id<Charger> chId = chSpec.getId();
            String hubId = (String) chSpec.getAttributes().getAttribute("hubId");
            if (hubId == null) throw new IllegalArgumentException("Charger " + chId + " senza hubId");

            charger2hub.put(chId, hubId);
            chargerEnergy.put(chId, 0.0);

            hubEnergy.putIfAbsent(hubId, 0.0);
            hubOccupancy.putIfAbsent(hubId, 0);
            hubDirtyFlags.putIfAbsent(hubId, true); // segnala inizialmente come dirty
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
        hubDirtyFlags.putIfAbsent(hubId, true);
    }

    // ------------------------
    // Metodi di aggiornamento con dirty flag
    // ------------------------
    public void incrementOccupancy(String hubId) {
        hubOccupancy.put(hubId, hubOccupancy.get(hubId) + 1);
        hubDirtyFlags.put(hubId, true);
    }

    public void decrementOccupancy(String hubId) {
        hubOccupancy.put(hubId, hubOccupancy.get(hubId) - 1);
        hubDirtyFlags.put(hubId, true);
    }

    public void addChargerEnergy(Id<Charger> chargerId, double energy) {
        chargerEnergy.put(chargerId, chargerEnergy.get(chargerId) + energy);
        String hubId = charger2hub.get(chargerId);
        if (hubId != null) hubDirtyFlags.put(hubId, true);
    }

    public void addHubEnergy(String hubId, double energy) {
        hubEnergy.put(hubId, hubEnergy.get(hubId) + energy);
        hubDirtyFlags.put(hubId, true);
    }

    public void recordTimeline(double time) {
        Map<String, Integer> snapshot = new HashMap<>(hubOccupancy);
        timeline.put(time, snapshot);
    }

    // ------------------------
    // Accessor
    // ------------------------
    public double getChargerEnergy(Id<Charger> chargerId) { return chargerEnergy.get(chargerId); }
    public double getHubEnergy(String hubId) { return hubEnergy.get(hubId); }
    public int getHubOccupancy(String hubId) { return hubOccupancy.get(hubId); }
    public Map<String, Double> getHubEnergyMap() { return hubEnergy; }
    public Map<String, Integer> getHubOccupancyMap() { return hubOccupancy; }
    public Map<Double, Map<String, Integer>> getTimeline() { return timeline; }

    public String getHubIdForCharger(Id<Charger> chargerId) { return charger2hub.get(chargerId); }

    // ------------------------
    // Dirty flags
    // ------------------------
    public boolean isDirty(String hubId) {
        return hubDirtyFlags.getOrDefault(hubId, false);
    }

    public void resetDirtyFlags() {
        hubDirtyFlags.replaceAll((k, v) -> false);
    }

    public Set<String> getDirtyHubs() {
        Set<String> dirtyHubs = new HashSet<>();
        for (Map.Entry<String, Boolean> entry : hubDirtyFlags.entrySet()) {
            if (entry.getValue()) dirtyHubs.add(entry.getKey());
        }
        return dirtyHubs;
    }

    public void resetAll() {
        chargerEnergy.replaceAll((k,v) -> 0.0);
        hubEnergy.replaceAll((k,v) -> 0.0);
        hubOccupancy.replaceAll((k,v) -> 0);
        timeline.clear();
        hubDirtyFlags.replaceAll((k,v) -> true);
    }
}
