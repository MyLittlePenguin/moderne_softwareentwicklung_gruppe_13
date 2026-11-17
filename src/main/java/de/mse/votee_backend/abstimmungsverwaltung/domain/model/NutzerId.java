package de.mse.votee_backend.abstimmungsverwaltung.domain.model;

/**
 * Value Object zur Identifikation eines Nutzers (z. B. der Verwaltungsangestellten).
 */
public record NutzerId(String wert) {

    public NutzerId {
        if (wert == null || wert.isBlank()) {
            throw new AbstimmungsregelVerletztException("NutzerId darf nicht leer sein.");
        }
    }
}
