package de.mse.votee_backend.abstimmungsverwaltung.domain.model;

import java.time.LocalDateTime;

/**
 * Value Object für den Zeitraum einer Abstimmung.
 * Regel 4: Das Ende des Zeitraums liegt nach dem Beginn.
 */
public record Abstimmungszeitraum(LocalDateTime beginn, LocalDateTime ende) {

    public Abstimmungszeitraum {
        if (beginn == null) {
            throw new IllegalArgumentException("Beginn darf nicht null sein.");
        }
        if (ende == null) {
            throw new IllegalArgumentException("Ende darf nicht null sein.");
        }
        if (!ende.isAfter(beginn)) {
            throw new IllegalArgumentException("Das Ende muss nach dem Beginn liegen.");
        }
    }
}
