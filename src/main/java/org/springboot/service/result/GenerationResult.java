package org.springboot.service.result;

/**
 * Enum rappresentante i possibili esiti della generazione di flotta o hub.
 */
public enum GenerationResult {
    SUCCESS("Generazione completata con successo"),
    INVALID_REQUEST("Richiesta invalida"),
    FILE_NOT_FOUND("File non trovato"),
    ERROR("Errore nella generazione");

    private final String message;

    GenerationResult(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
