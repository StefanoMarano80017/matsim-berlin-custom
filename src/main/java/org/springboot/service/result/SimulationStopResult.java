package org.springboot.service.result;

/**
 * Enum rappresentante i possibili esiti dell'arresto della simulazione.
 */
public enum SimulationStopResult {
    SUCCESS("Richiesta di interruzione inviata con successo"),
    NOT_RUNNING("Nessuna simulazione attiva"),
    ERROR("Errore nell'arresto della simulazione");

    private final String message;

    SimulationStopResult(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
