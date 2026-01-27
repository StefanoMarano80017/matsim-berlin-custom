package org.springboot.service.result;

/**
 * Enum rappresentante i possibili esiti dell'aggiornamento dello stato di una colonnina.
 */
public enum ChargerStateUpdateResult {
    SUCCESS("Stato della colonnina aggiornato con successo"),
    SIMULATION_NOT_RUNNING("Simulazione non in esecuzione"),
    INVALID_CHARGER_ID("ID colonnina non valido"),
    ERROR("Errore nell'aggiornamento dello stato");

    private final String message;

    ChargerStateUpdateResult(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
