package org.matsim.CustomEvModule.Hub;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.springframework.core.io.Resource;

import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HubManager {

    private static final Logger log = LogManager.getLogger(HubManager.class);

    private final HubGenerator generator;
    private final ChargingInfrastructureSpecification infraSpec;

    private Map<String, ChargingHub> hubs = new HashMap<>();
    private final Map<Id<Charger>, String> charger2hub = new HashMap<>();

    public HubManager(Network network, ChargingInfrastructureSpecification infraSpec) {
        this.infraSpec = infraSpec;
        this.generator = new HubGenerator(network);
    }

    /**
     * Restituisce una lista di charger libero e compatibile con i tipi richiesti su uno specifico link.
     * @param linkId link su cui cercare charger
     * @param compatibleTypes tipi di charger compatibili con il veicolo
     * @return Optional<Id<Charger>> disponibile
     */
    public List<Charger> getAvailableChargerForLink(Id<Link> linkId, ImmutableList<String> compatibleTypes, ChargingInfrastructure chargingInfrastructure) {
        return chargingInfrastructure.getChargers().values().stream()
                .filter(c -> c.getLink().getId().equals(linkId))
                .filter(c -> compatibleTypes.contains(
                    c.getSpecification().getChargerType()
                ))
                .toList();
    }

    public void createHub(Resource csvFile) {
        try {
            this.hubs = generator.generateHubsFromCSV(csvFile, infraSpec);
            log.debug("[HubManager] Hub e colonnine generate con successo.");
        } catch (IOException e) {
            e.printStackTrace();
        }

        infraSpec.getChargerSpecifications().values().forEach(chSpec -> {
            Id<Charger> chId = chSpec.getId();
            String hubId = (String) chSpec.getAttributes().getAttribute("hubId");
            if (hubId == null) throw new IllegalArgumentException("Charger " + chId + " senza hubId");
            charger2hub.put(chId, hubId);
        });

        log.debug("[HubManager] Hub e colonnine create e registrate.");
    }

    public ChargingHub getHub(String hubId) {
        return hubs.get(hubId);
    }

    public Collection<ChargingHub> getChargingHubs() {
        return hubs.values();
    }

    public String getHubIdForCharger(Id<Charger> chargerId) {
        return charger2hub.get(chargerId);
    }

    public void incrementOccupancy(Id<Charger> chargerId, String evId, double energy) {
        ChargingHub hub = getHub(getHubIdForCharger(chargerId));
        hub.incrementOccupancy(chargerId, evId, energy);
    }

    public void decrementOccupancy(Id<Charger> chargerId, double energy) {
        ChargingHub hub = getHub(getHubIdForCharger(chargerId));
        hub.decrementOccupancy(chargerId, energy);
    }

    public Collection<ChargingHub> getAllHubs() {
        return Collections.unmodifiableCollection(hubs.values());
    }

    // ---------------- Dirty flags ----------------
    public Set<ChargingHub> getDirtyHubs() {
        return hubs.values().stream()
                   .filter(ChargingHub::isDirty)
                   .collect(Collectors.toSet());
    }

    public void resetDirtyFlags() {
        hubs.values().forEach(ChargingHub::resetDirty);
    }
}
