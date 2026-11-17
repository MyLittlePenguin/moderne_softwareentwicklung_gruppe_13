package de.mse.votee_backend.abstimmungsverwaltung.domain.model;

import java.util.UUID;

/**
 * Value Object zur eindeutigen Identifikation einer Abstimmung (Aggregate Root).
 */
public record AbstimmungId(String wert) {

    public AbstimmungId {
        if (wert == null || wert.isBlank()) {
            throw new IllegalArgumentException("AbstimmungId darf nicht leer sein.");
        }
    }

    /**
     * Erzeugt eine neue, zufällige AbstimmungId basierend auf einer UUID.
     *
     * @return eine neue AbstimmungId
     */
    public static AbstimmungId neu() {
        return new AbstimmungId(UUID.randomUUID().toString());
    }
}
