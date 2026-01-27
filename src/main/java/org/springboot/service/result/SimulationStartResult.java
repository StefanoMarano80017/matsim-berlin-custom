package org.springboot.service.result;

/**
 * Enum rappresentante i possibili esiti dell'avvio della simulazione.
 */
public enum SimulationStartResult {
    SUCCESS("Simulazione avviata con successo"),
    ALREADY_RUNNING("Simulazione già in esecuzione"),
    FLEET_NOT_GENERATED("Flotta non generata"),
    HUBS_NOT_GENERATED("Hub non generati"),
    FLEET_AND_HUBS_NOT_GENERATED("Flotta e hub non generati"),
    ERROR("Errore nell'avvio della simulazione");

    private final String message;

    SimulationStartResult(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
