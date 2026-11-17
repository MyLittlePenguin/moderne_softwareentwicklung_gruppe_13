package de.mse.votee_backend.abstimmungsverwaltung.domain.model;

/**
 * Value Object für den Titel einer Abstimmung (englischsprachige Schreibweise / Alias für {@link Titel}).
 */
public record Title(String wert) {

    public Title {
        wert = new Titel(wert).wert();
    }
}
