package de.mse.votee_backend.abstimmungsverwaltung.domain.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Aggregate Root für eine Abstimmung.
 */
public final class Abstimmung {

    private final NutzerId erstelltVon;
    private final String titel;
    private final String beschreibung;
    private final Abstimmungszeitraum zeitraum;
    private final List<AbstimmOption> optionen;

    private Abstimmung(NutzerId erstelltVon,
                       String titel,
                       String beschreibung,
                       Abstimmungszeitraum zeitraum,
                       List<AbstimmOption> optionen) {
        this.erstelltVon = erstelltVon;
        this.titel = titel;
        this.beschreibung = beschreibung;
        this.zeitraum = zeitraum;
        this.optionen = optionen;
    }

    /**
     * Erstellt eine neue Abstimmung nach Prüfung aller Fachregeln (Invarianten).
     *
     * @param erstelltVon  die NutzerId der Erstellerin / des Erstellers
     * @param titel        der Titel der Abstimmung
     * @param beschreibung die Beschreibung (optional)
     * @param zeitraum     der Abstimmungszeitraum
     * @param optionen     die Liste der Antwortoptionen (mindestens zwei)
     * @return die erstellte Abstimmung
     */
    public static Abstimmung erstelle(NutzerId erstelltVon,
                                      String titel,
                                      String beschreibung,
                                      Abstimmungszeitraum zeitraum,
                                      List<AbstimmOption> optionen) {
        // Regel 1: Ersteller ist gesetzt
        if (erstelltVon == null) {
            throw new IllegalArgumentException("Ersteller (NutzerId) muss angegeben werden.");
        }

        // Regel 2: Titel ist nicht leer
        if (titel == null || titel.isBlank()) {
            throw new IllegalArgumentException("Titel darf nicht leer sein.");
        }

        // Regel 3: Beschreibung ist optional (null oder blank wird zu leerem String)
        String bereinigteBeschreibung = (beschreibung == null || beschreibung.isBlank()) ? "" : beschreibung;

        // Regel 4: Zeitraum ist gesetzt
        if (zeitraum == null) {
            throw new IllegalArgumentException("Abstimmungszeitraum muss angegeben werden.");
        }

        // Regel 5: Optionen prüfen
        if (optionen == null) {
            throw new IllegalArgumentException("Optionsliste darf nicht null sein.");
        }
        if (optionen.size() < 2) {
            throw new IllegalArgumentException("Eine Abstimmung muss mindestens zwei Optionen haben.");
        }

        Set<String> normalisierteTexte = new HashSet<>();
        for (AbstimmOption option : optionen) {
            if (option == null) {
                throw new IllegalArgumentException("Option darf nicht null sein.");
            }
            if (option.text() == null || option.text().isBlank()) {
                throw new IllegalArgumentException("Optionstext darf nicht leer sein.");
            }
            String normalisierterText = option.text().strip().toLowerCase(Locale.ROOT);
            if (!normalisierteTexte.add(normalisierterText)) {
                throw new IllegalArgumentException("Doppelte Option gefunden: " + option.text());
            }
        }

        return new Abstimmung(
                erstelltVon,
                titel,
                bereinigteBeschreibung,
                zeitraum,
                List.copyOf(optionen)
        );
    }

    public NutzerId getErstelltVon() {
        return erstelltVon;
    }

    public String getTitel() {
        return titel;
    }

    public String getBeschreibung() {
        return beschreibung;
    }

    public Abstimmungszeitraum getZeitraum() {
        return zeitraum;
    }

    public List<AbstimmOption> getOptionen() {
        return optionen;
    }

    public LocalDateTime getFrist() {
        return zeitraum.ende();
    }
}
