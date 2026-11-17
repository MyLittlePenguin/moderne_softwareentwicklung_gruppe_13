package de.mse.votee_backend.abstimmungsverwaltung.domain.model;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Value Object für den Titel einer Abstimmung.
 * Kapselt die fachlichen Invarianten bezüglich Länge, Zeichensatz und Normalisierung.
 */
public record Titel(String wert) {

    private static final int MIN_LAENGE = 3;
    private static final int MAX_LAENGE = 100;

    private static final Pattern TITEL_PATTERN =
            Pattern.compile("^(?=.*[\\p{L}0-9])[\\p{L}0-9 ,.?!:()'\"/%€-]{3,100}$");

    public Titel {
        if (wert == null || wert.isBlank()) {
            throw new IllegalArgumentException("Titel darf nicht leer sein.");
        }

        // Unicode normalisieren (NFD zu NFC, z. B. macOS zerlegte Umlaute)
        String normalisiert = Normalizer.normalize(wert, Normalizer.Form.NFC);

        // Rand-Leerzeichen entfernen (Steuerzeichen wie \t, \n bleiben erhalten und scheitern am Pattern)
        String bereinigt = stripSpaces(normalisiert);

        if (!TITEL_PATTERN.matcher(bereinigt).matches()) {
            if (bereinigt.length() < MIN_LAENGE) {
                throw new IllegalArgumentException("Titel muss mindestens " + MIN_LAENGE + " Zeichen lang sein.");
            }
            if (bereinigt.length() > MAX_LAENGE) {
                throw new IllegalArgumentException("Titel darf höchstens " + MAX_LAENGE + " Zeichen lang sein.");
            }
            throw new IllegalArgumentException("Titel enthält ungültige Zeichen.");
        }

        wert = bereinigt;
    }

    private static String stripSpaces(String str) {
        int start = 0;
        while (start < str.length() && str.charAt(start) == ' ') {
            start++;
        }
        int end = str.length();
        while (end > start && str.charAt(end - 1) == ' ') {
            end--;
        }
        return str.substring(start, end);
    }
}
