package de.mse.votee_backend.kontoverwaltung.domain.model;

/**
 * Fachliche Rollen innerhalb der Kontoverwaltung.
 */
public enum Rolle {
    /** Verwaltungsangestellte / Administrator:innen, die Abstimmungen erstellen dürfen. */
    ADMIN,
    /** Bürger:innen, die an Abstimmungen teilnehmen dürfen. */
    BUERGER
}
