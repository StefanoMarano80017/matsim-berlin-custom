package org.matsim.CustomEvModule.Hub;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.infrastructure.Charger;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ChargingHub {

    private final String hubId;
    private final Id<Link> linkId;

    private final Set<Id<Charger>> chargers = new HashSet<>();

    /**
     * Charger occupati -> EV che li sta occupando
     */
    private final Map<Id<Charger>, String> occupiedChargers = new HashMap<>();

    private final Map<Id<Charger>, Double> chargerEnergy = new HashMap<>();

    private double totalEnergy = 0.0;
    private boolean dirty = true;

    public ChargingHub(String hubId, Id<Link> linkId) {
        this.hubId = hubId;
        this.linkId = linkId;
    }

    public String getId() {
        return hubId;
    }

    // -------------------- MUTATOR METHODS --------------------

    public synchronized void addCharger(Id<Charger> chargerId) {
        chargers.add(chargerId);
        chargerEnergy.putIfAbsent(chargerId, 0.0);
        dirty = true;
    }

    public synchronized void removeCharger(Id<Charger> chargerId) {
        chargers.remove(chargerId);
        occupiedChargers.remove(chargerId);
        chargerEnergy.remove(chargerId);
        dirty = true;
    }

    /**
     * Segna un charger come occupato da un EV
     */
    public synchronized void incrementOccupancy(Id<Charger> chargerId, String evId, double energy) {
        if (!chargers.contains(chargerId)) {
            throw new IllegalArgumentException("Charger non appartiene all'hub: " + chargerId);
        }

        if (occupiedChargers.containsKey(chargerId)) {
            throw new IllegalStateException("Charger già occupato: " + chargerId);
        }

        occupiedChargers.put(chargerId, evId);
        addChargerEnergy(chargerId, energy);
        dirty = true;
    }

    /**
     * Libera un charger
     */
    public synchronized void decrementOccupancy(Id<Charger> chargerId, double energy) {
        occupiedChargers.remove(chargerId);
        addChargerEnergy(chargerId, energy);
        dirty = true;
    }

    private synchronized void addChargerEnergy(Id<Charger> chargerId, double energy) {
        chargerEnergy.put(chargerId, chargerEnergy.getOrDefault(chargerId, 0.0) + energy);
        totalEnergy += energy;
    }

    // -------------------- ACCESSOR METHODS --------------------
    public synchronized Set<Id<Charger>> getChargers() {
        return Collections.unmodifiableSet(new HashSet<>(chargers));
    }

    /**
     * Charger occupati
     */
    public synchronized Set<Id<Charger>> getOccupiedChargers() {
        return Collections.unmodifiableSet(new HashSet<>(occupiedChargers.keySet()));
    }

    /**
     * Mappa completa charger -> evId
     */
    public synchronized Map<Id<Charger>, String> getOccupiedChargersWithEv() {
        return Collections.unmodifiableMap(new HashMap<>(occupiedChargers));
    }

    public synchronized int getOccupancy() {
        return occupiedChargers.size();
    }

    public synchronized double getTotalEnergy() {
        return totalEnergy;
    }

    public synchronized boolean containsCharger(Id<Charger> chargerId) {
        return chargers.contains(chargerId);
    }

    public synchronized double getChargerEnergy(Id<Charger> chargerId) {
        return chargerEnergy.getOrDefault(chargerId, 0.0);
    }

    public synchronized Id<Link> getLink() {
        return this.linkId;
    }

    /**
     * Restituisce l'EV che occupa un charger (null se libero)
     */
    public synchronized String getEvOccupyingCharger(Id<Charger> chargerId) {
        return occupiedChargers.get(chargerId);
    }

    // -------------------- Dirty flag --------------------

    public synchronized boolean isDirty() {
        return dirty;
    }

    public synchronized void resetDirty() {
        dirty = false;
    }
}
