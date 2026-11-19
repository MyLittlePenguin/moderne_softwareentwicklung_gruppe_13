package de.mse.votee_backend.abstimmungsverwaltung.application;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.regex.Pattern;

import de.mse.votee_backend.abstimmungsverwaltung.domain.model.AbstimmungsregelVerletztException;
import de.mse.votee_backend.abstimmungsverwaltung.domain.model.Titel;

/**
 * Validierung von Benutzereingaben aus Formularen vor der Umwandlung in Domänenobjekte.
 */
public class AbstimmungEingabeValidator {

    private static final Pattern DATUM_PATTERN =
            Pattern.compile("^(0[1-9]|[12][0-9]|3[01])\\.(0[1-9]|1[0-2])\\.20[0-9]{2} ([01][0-9]|2[0-3]):[0-5][0-9]$");

    private static final DateTimeFormatter DATUM_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT);

    /**
     * Prüft, ob ein gegebener Titel den Eingaberegeln entspricht.
     * Delegiert an das Domänen-Value-Object {@link Titel}.
     *
     * @param titel der zu prüfende Titel
     * @return true, falls gültig, sonst false
     */
    public boolean istGueltigerTitel(String titel) {
        if (titel == null) {
            return false;
        }
        try {
            new Titel(titel);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Prüft, ob ein Datumsstring das korrekte Format hat und ein existierendes Datum darstellt.
     *
     * @param datum der Datumsstring im Format dd.MM.yyyy HH:mm
     * @return true, falls gültig, sonst false
     */
    public boolean istGueltigesDatum(String datum) {
        try {
            parseDatum(datum);
            return true;
        } catch (AbstimmungsregelVerletztException e) {
            return false;
        }
    }

    /**
     * Parst einen Datumsstring im Format {@code dd.MM.yyyy HH:mm} (Jahre 2000–2099) mit strikter Kalenderprüfung.
     *
     * @param datum der zu parsende Datumsstring
     * @return der geparste Zeitpunkt
     * @throws AbstimmungsregelVerletztException falls Format oder Kalenderdatum ungültig sind
     */
    public LocalDateTime parseDatum(String datum) {
        if (datum == null || !DATUM_PATTERN.matcher(datum).matches()) {
            throw new AbstimmungsregelVerletztException(
                    "Ungültiges Datumsformat. Erwartet wird dd.MM.yyyy HH:mm (Jahre 2000–2099).");
        }

        try {
            return LocalDateTime.parse(datum, DATUM_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new AbstimmungsregelVerletztException("Ungültiges Kalenderdatum: " + datum);
        }
    }
}
