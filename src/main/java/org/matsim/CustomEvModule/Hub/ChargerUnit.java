package org.matsim.CustomEvModule.Hub;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.infrastructure.Charger;

import java.util.Collections;
import java.util.Set;

/**
 * Rappresenta una singola colonnina di ricarica (charger) all'interno di un hub.
 * Traccia:
 * - Energia cumulativa totale erogata
 * - Energia attualmente in erogazione (durante un timestep)
 * - L'EV che occupa il charger (se presente)
 * - I plug disponibili
 */
public class ChargerUnit {

    private final Id<Charger> chargerId;
    private final Set<String> plugs;
    
    private double cumulativeEnergyDelivered = 0.0;
    private double currentEnergyDelivering = 0.0;
    private String occupyingEvId = null;

    private boolean active = true;

    /**
     * Costruisce una unità charger
     * 
     * @param chargerId ID della colonnina
     * @param plugs Set di tipologie di plug disponibili
     */
    public ChargerUnit(Id<Charger> chargerId, Set<String> plugs) {
        this.chargerId = chargerId;
        this.plugs = plugs != null ? plugs : Set.of();
    }

    /**
     * Costruisce una unità charger senza plugs
     * 
     * @param chargerId ID della colonnina
     */
    public ChargerUnit(Id<Charger> chargerId) {
        this(chargerId, null);
    }

    // ================== GETTER ==================
    
    public Id<Charger> getChargerId() {
        return chargerId;
    }

    public Set<String> getPlugs() {
        return Collections.unmodifiableSet(plugs);
    }

    public double getCumulativeEnergyDelivered() {
        return cumulativeEnergyDelivered;
    }

    public double getCurrentEnergyDelivering() {
        return currentEnergyDelivering;
    }

    public String getOccupyingEvId() {
        return occupyingEvId;
    }

    public boolean isOccupied() {
        return occupyingEvId != null;
    }

    public boolean isActive() {
        return active;
    }

    // ================== SETTER ==================

    /**
     * Assegna un EV alla colonnina (occupazione)
     * 
     * @param evId ID dell'EV che occupa la colonnina
     * @throws IllegalStateException se la colonnina è già occupata
     */
    public synchronized void setOccupyingEv(String evId) {
        if (!active) {
            throw new IllegalStateException("Charger disattivato: " + chargerId);
        }
        if (occupyingEvId != null && !occupyingEvId.equals(evId)) {
            throw new IllegalStateException("Charger già occupato da: " + occupyingEvId);
        }
        this.occupyingEvId = evId;
    }

    /**
     * Libera la colonnina (rimozione dell'EV)
     */
    public synchronized void releaseOccupyingEv() {
        this.occupyingEvId = null;
    }

    /**
     * Aggiorna l'energia in erogazione in questo timestep.
     * Viene resettato a ogni inizio timestep.
     * 
     * @param energy Energia erogata in questo timestep
     */
    public synchronized void setCurrentEnergyDelivering(double energy) {
        if (!active) {
            this.currentEnergyDelivering = 0.0;
            return;
        }
        this.currentEnergyDelivering = energy;
    }

    /**
     * Resetta l'energia in erogazione (da chiamare all'inizio di ogni timestep)
     */
    public synchronized void resetCurrentEnergyDelivering() {
        this.currentEnergyDelivering = 0.0;
    }

    /**
     * Aggiunge energia all'energia cumulativa erogata.
     * Questa energia è quella che viene definitivamente erogata quando il charging termina.
     * 
     * @param energy Energia da aggiungere alla cumulativa
     */
    public synchronized void addCumulativeEnergyDelivered(double energy) {
        this.cumulativeEnergyDelivered += energy;
    }

    public synchronized void setActive(boolean active) {
        this.active = active;
        // Se viene disattivato mentre è occupato → eccezione 
        if (!active && occupyingEvId != null) {
           throw new IllegalStateException("Charger disattivato disattivato mentre è occupato: " + chargerId);
        }
        resetCurrentEnergyDelivering();
    }


    @Override
    public String toString() {
        return String.format(
            "ChargerUnit{id=%s, occupied=%b, ev=%s, cumEnergy=%.2f, currEnergy=%.2f}",
            chargerId,
            isOccupied(),
            occupyingEvId,
            cumulativeEnergyDelivered,
            currentEnergyDelivering
        );
    }
}
