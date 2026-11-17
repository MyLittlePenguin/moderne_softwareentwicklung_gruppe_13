package de.mse.votee_backend.abstimmungsverwaltung.domain.model;

/**
 * Fachliche Exception: Eine Regel (Invariante) der Abstimmungsverwaltung ist verletzt,
 * z. B. weniger als zwei Optionen oder ein Ende vor dem Beginn.
 *
 * Erbt von {@link IllegalArgumentException}, weil es sich um ungültige Eingaben handelt.
 * So können Aufrufer gezielt fachliche Fehler abfangen, ohne dass bestehender Code bricht.
 */
public class AbstimmungsregelVerletztException extends IllegalArgumentException {

    public AbstimmungsregelVerletztException(String meldung) {
        super(meldung);
    }
}
