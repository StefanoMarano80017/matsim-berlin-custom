package org.springboot.service;

/**
 * Enum che rappresenta gli stati possibili della simulazione
 */
public enum SimulationState {
    IDLE("Idle"),
    RUNNING("In esecuzione"),
    COMPLETED("Completata"),
    ERROR("Errore"),
    STOPPED("Interrotta");

    private final String description;

    SimulationState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
