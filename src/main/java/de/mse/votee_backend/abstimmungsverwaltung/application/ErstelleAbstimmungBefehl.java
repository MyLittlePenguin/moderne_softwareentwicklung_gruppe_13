package de.mse.votee_backend.abstimmungsverwaltung.application;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Command-Objekt (Anwendungsfall-Eingabe) für das Erstellen einer neuen Abstimmung.
 *
 * @param titel        der Titel der Abstimmung
 * @param beschreibung die optionale Beschreibung
 * @param beginn       der Startzeitpunkt der Abstimmung
 * @param ende         das Fristende der Abstimmung
 * @param optionen     die Liste der Antwortoptionen (als Rohtexte)
 */
public record ErstelleAbstimmungBefehl(
        String titel,
        String beschreibung,
        LocalDateTime beginn,
        LocalDateTime ende,
        List<String> optionen) {
}
