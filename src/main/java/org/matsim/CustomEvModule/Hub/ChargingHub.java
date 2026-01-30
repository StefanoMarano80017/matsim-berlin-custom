package org.matsim.CustomEvModule.Hub;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.ev.infrastructure.Charger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ChargingHub {

    private final String hubId;
    private final Id<Link> linkId;
    private double coordX;
    private double coordY;    

    /**
     * Mappa charger ID -> ChargerUnit
     * Contiene tutte le unità di ricarica dell'hub con i loro stati
     */
    private final Map<Id<Charger>, ChargerUnit> chargerUnits = new HashMap<>();

    private double totalCumulativeEnergy = 0.0;
    private boolean dirty = true;

    public ChargingHub(String hubId, Id<Link> linkId, double coordX, double coordY) {
        this.hubId = hubId;
        this.linkId = linkId;
        this.coordX = coordX;
        this.coordY = coordY;
    }

    public String getId() {
        return hubId;
    }

    public double getCoordX() {
        return coordX;
    }

    public double getCoordY() {
        return coordY;
    }

    // -------------------- MUTATOR METHODS --------------------

    /**
     * Aggiunge una colonnina all'hub con plugs specifici
     * 
     * @param chargerId ID della colonnina
     * @param plugs Set di tipologie di plug disponibili
     */
    public synchronized void addCharger(Id<Charger> chargerId, Set<String> plugs) {
        ChargerUnit unit = new ChargerUnit(chargerId, plugs);
        chargerUnits.put(chargerId, unit);
        dirty = true;
    }

    /**
     * Aggiunge una colonnina all'hub senza plugs specifici
     * 
     * @param chargerId ID della colonnina
     */
    public synchronized void addCharger(Id<Charger> chargerId) {
        ChargerUnit unit = new ChargerUnit(chargerId);
        chargerUnits.put(chargerId, unit);
        dirty = true;
    }

    /**
     * Rimuove una colonnina dall'hub
     * 
     * @param chargerId ID della colonnina da rimuovere
     */
    public synchronized void removeCharger(Id<Charger> chargerId) {
        ChargerUnit unit = chargerUnits.remove(chargerId);
        if (unit != null) {
            totalCumulativeEnergy -= unit.getCumulativeEnergyDelivered();
        }
        dirty = true;
    }

    /**
     * Segna una colonnina come occupata da un EV (inizio ricarica)
     * 
     * @param chargerId ID della colonnina
     * @param evId ID dell'EV che inizia la ricarica
     */
    public synchronized void incrementOccupancy(Id<Charger> chargerId, String evId) {
        ChargerUnit unit = chargerUnits.get(chargerId);
        if (unit == null) {
            throw new IllegalArgumentException("Charger non appartiene all'hub: " + chargerId);
        }
        unit.setOccupyingEv(evId);
        dirty = true;
    }

    /**
     * Libera una colonnina e registra l'energia erogata (fine ricarica)
     * 
     * @param chargerId ID della colonnina
     * @param energy Energia erogata durante la ricarica
     */
    public synchronized void decrementOccupancy(Id<Charger> chargerId, double energy) {
        ChargerUnit unit = chargerUnits.get(chargerId);
        if (unit != null) {
            unit.releaseOccupyingEv();
            unit.addCumulativeEnergyDelivered(energy);
            totalCumulativeEnergy += energy;
            dirty = true;
        }
    }

    /**
     * Aggiorna l'energia che la colonnina sta erogando in questo timestep.
     * Chiamato da TimeStepSocMonitor ad ogni timestep.
     * 
     * @param chargerId ID della colonnina
     * @param energy Energia erogata in questo timestep
     */
    public synchronized void updateChargerEnergyDelivering(Id<Charger> chargerId, double energy) {
        ChargerUnit unit = chargerUnits.get(chargerId);
        if (unit != null) {
            unit.setCurrentEnergyDelivering(energy);
            dirty = true;
        }
    }

    /**
     * Resetta l'energia in erogazione per tutte le colonnine.
     * Deve essere chiamato all'inizio di ogni timestep.
     */
    public synchronized void resetCurrentEnergyDelivering() {
        chargerUnits.values().forEach(ChargerUnit::resetCurrentEnergyDelivering);
    }

    public synchronized void setChargerActive(Id<Charger> chargerId, boolean active) {
        ChargerUnit unit = getChargerUnit(chargerId);
        if (unit == null) {
            throw new IllegalArgumentException("Charger non presente nell'hub: " + chargerId);
        }
        unit.setActive(active);
        dirty = true;
    }

    //attiva disattiva intero hub
    public synchronized void setAllChargersActive(boolean active) {
        chargerUnits.values().forEach(unit -> unit.setActive(active));
        dirty = true;
    }

    // -------------------- ACCESSOR METHODS --------------------

    /**
     * Ottieni una ChargerUnit specifica
     * 
     * @param chargerId ID della colonnina
     * @return La ChargerUnit o null se non presente
     */
    public synchronized ChargerUnit getChargerUnit(Id<Charger> chargerId) {
        return chargerUnits.get(chargerId);
    }

    /**
     * Ritorna tutte le ChargerUnit dell'hub
     * 
     * @return Lista immutabile di ChargerUnit
     */
    public synchronized List<ChargerUnit> getChargerUnits() {
        return Collections.unmodifiableList(
            new java.util.ArrayList<>(chargerUnits.values())
        );
    }

    /**
     * Ritorna gli ID di tutte le colonnine
     * 
     * @return Set immutabile degli ID
     */
    public synchronized Set<Id<Charger>> getChargersId() {
        return Collections.unmodifiableSet(new HashSet<>(chargerUnits.keySet()));
    }

    /**
     * Ritorna tutti gli Id dei ChargerUnit attive dell'hub
     * 
     * @return Lista immutabile di ChargerUnit
     */
    public synchronized Set<Id<Charger>> getActiveChargersId() {
        return   getChargerUnits().stream()
                .filter(ChargerUnit::isActive)
                .map(ChargerUnit::getChargerId)
                .collect(Collectors.toSet());
    }

    /**
     * Ritorna i plug disponibili per una colonnina
     * 
     * @param chargerId ID della colonnina
     * @return Set di plug o Set vuoto se non presente
     */
    public synchronized Set<String> getPlugs(Id<Charger> chargerId) {
        ChargerUnit unit = chargerUnits.get(chargerId);
        return unit != null ? unit.getPlugs() : Set.of();
    }

    /**
     * Ritorna gli ID delle colonnine occupate
     * 
     * @return Set immutabile degli ID occupati
     */
    public synchronized Set<Id<Charger>> getOccupiedChargers() {
        return chargerUnits.values().stream()
            .filter(ChargerUnit::isOccupied)
            .map(ChargerUnit::getChargerId)
            .collect(Collectors.toSet());
    }

    /**
     * Mappa charger ID -> EV che lo occupa
     * 
     * @return Mappa immutabile
     */
    public synchronized Map<Id<Charger>, String> getOccupiedChargersWithEv() {
        return chargerUnits.values().stream()
            .filter(ChargerUnit::isOccupied)
            .collect(Collectors.toUnmodifiableMap(
                ChargerUnit::getChargerId,
                ChargerUnit::getOccupyingEvId
            ));
    }

    /**
     * Numero di colonnine occupate
     * 
     * @return Numero di occupancy
     */
    public synchronized int getOccupancy() {
        return (int) chargerUnits.values().stream()
            .filter(ChargerUnit::isOccupied)
            .count();
    }

    /**
     * Energia cumulativa totale erogata da tutte le colonnine
     * 
     * @return Energia totale in Joule
     */
    public synchronized double getTotalEnergy() {
        return totalCumulativeEnergy;
    }

    /**
     * Controlla se l'hub contiene una colonnina
     * 
     * @param chargerId ID della colonnina
     * @return true se presente, false altrimenti
     */
    public synchronized boolean containsCharger(Id<Charger> chargerId) {
        return chargerUnits.containsKey(chargerId);
    }

    /**
     * Energia cumulativa erogata da una specifica colonnina
     * 
     * @param chargerId ID della colonnina
     * @return Energia cumulativa in Joule
     */
    public synchronized double getChargerEnergy(Id<Charger> chargerId) {
        ChargerUnit unit = chargerUnits.get(chargerId);
        return unit != null ? unit.getCumulativeEnergyDelivered() : 0.0;
    }

    /**
     * Energia attualmente in erogazione da una colonnina in questo timestep
     * 
     * @param chargerId ID della colonnina
     * @return Energia in erogazione in Joule
     */
    public synchronized double getChargerCurrentEnergyDelivering(Id<Charger> chargerId) {
        ChargerUnit unit = chargerUnits.get(chargerId);
        return unit != null ? unit.getCurrentEnergyDelivering() : 0.0;
    }

    /**
     * Link dove si trova l'hub
     * 
     * @return ID del link
     */
    public synchronized Id<Link> getLink() {
        return this.linkId;
    }

    /**
     * EV che occupa una colonnina
     * 
     * @param chargerId ID della colonnina
     * @return ID dell'EV o null se libera
     */
    public synchronized String getEvOccupyingCharger(Id<Charger> chargerId) {
        ChargerUnit unit = chargerUnits.get(chargerId);
        return unit != null ? unit.getOccupyingEvId() : null;
    }

    // -------------------- Dirty flag --------------------

    public synchronized boolean isDirty() {
        return dirty;
    }

    public synchronized void resetDirty() {
        dirty = false;
    }
}
