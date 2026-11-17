package de.mse.votee_backend.abstimmungsverwaltung.domain.model;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Value Object für eine Antwortoption einer Abstimmung.
 * Regel 5: Der Text einer Option ist nicht leer.
 */
public record AbstimmOption(String text) {

    public AbstimmOption {
        if (text == null || text.isBlank()) {
            throw new AbstimmungsregelVerletztException("Text einer Option darf nicht leer sein.");
        }
    }

    /**
     * Liefert den Text in einer Vergleichsform: Unicode NFC, ohne Rand-Leerzeichen, kleingeschrieben.
     * Zwei Optionen mit gleichem normalisierten Text gelten fachlich als dieselbe Option.
     *
     * @return der normalisierte Vergleichstext
     */
    public String normalisierterText() {
        return Normalizer.normalize(text, Normalizer.Form.NFC).strip().toLowerCase(Locale.ROOT);
    }
}
