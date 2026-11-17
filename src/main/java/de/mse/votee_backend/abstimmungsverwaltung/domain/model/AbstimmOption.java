package de.mse.votee_backend.abstimmungsverwaltung.domain.model;

/**
 * Value Object für eine Antwortoption einer Abstimmung.
 * Regel 5: Der Text einer Option ist nicht leer.
 */
public record AbstimmOption(String text) {

    public AbstimmOption {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text einer Option darf nicht leer sein.");
        }
    }
}
