package de.mse.votee_backend.abstimmungsverwaltung.domain.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Aggregate Root für eine Abstimmung.
 */
public final class Abstimmung {

    private final AbstimmungId id;
    private final NutzerId erstelltVon;
    private final Titel titel;
    private final String beschreibung;
    private final Abstimmungszeitraum zeitraum;
    private final List<AbstimmOption> optionen;

    private Abstimmung(AbstimmungId id,
                       NutzerId erstelltVon,
                       Titel titel,
                       String beschreibung,
                       Abstimmungszeitraum zeitraum,
                       List<AbstimmOption> optionen) {
        this.id = id;
        this.erstelltVon = erstelltVon;
        this.titel = titel;
        this.beschreibung = beschreibung;
        this.zeitraum = zeitraum;
        this.optionen = optionen;
    }

    /**
     * Erstellt eine neue Abstimmung mit einem {@link Titel}-Objekt nach Prüfung aller Fachregeln (Invarianten).
     *
     * @param id           die eindeutige ID der Abstimmung
     * @param erstelltVon  die NutzerId der Erstellerin / des Erstellers
     * @param titel        das Value Object des Titels
     * @param beschreibung die Beschreibung (optional)
     * @param zeitraum     der Abstimmungszeitraum
     * @param optionen     die Liste der Antwortoptionen (mindestens zwei)
     * @return die erstellte Abstimmung
     */
    public static Abstimmung erstelle(AbstimmungId id,
                                      NutzerId erstelltVon,
                                      Titel titel,
                                      String beschreibung,
                                      Abstimmungszeitraum zeitraum,
                                      List<AbstimmOption> optionen) {
        // Regel 0: Id ist gesetzt
        if (id == null) {
            throw new IllegalArgumentException("AbstimmungId muss angegeben werden.");
        }

        // Regel 1: Ersteller ist gesetzt
        if (erstelltVon == null) {
            throw new IllegalArgumentException("Ersteller (NutzerId) muss angegeben werden.");
        }

        // Regel 2: Titel ist nicht leer
        if (titel == null) {
            throw new IllegalArgumentException("Titel darf nicht leer sein.");
        }

        // Regel 3: Beschreibung ist optional (null oder blank wird zu leerem String)
        String bereinigteBeschreibung = (beschreibung == null || beschreibung.isBlank()) ? "" : beschreibung;

        // Regel 4: Zeitraum ist gesetzt
        if (zeitraum == null) {
            throw new IllegalArgumentException("Abstimmungszeitraum muss angegeben werden.");
        }

        // Regel 5: mindestens zwei Optionen, keine Duplikate
        List<AbstimmOption> gepruefteOptionen = pruefeOptionen(optionen);

        return new Abstimmung(id, erstelltVon, titel, bereinigteBeschreibung, zeitraum, gepruefteOptionen);
    }

    /**
     * Prüft Regel 5 und liefert eine unveränderliche Kopie der Optionsliste.
     * Leere Optionstexte sind schon durch das Value Object {@link AbstimmOption} ausgeschlossen.
     *
     * @param optionen die übergebenen Optionen
     * @return unveränderliche Kopie der Optionen
     */
    private static List<AbstimmOption> pruefeOptionen(List<AbstimmOption> optionen) {
        if (optionen == null) {
            throw new IllegalArgumentException("Optionsliste darf nicht null sein.");
        }
        if (optionen.size() < 2) {
            throw new IllegalArgumentException("Eine Abstimmung muss mindestens zwei Optionen haben.");
        }

        // Bewusst Schleife statt Stream: bricht beim ersten Duplikat ab und nennt es in der Meldung
        Set<String> normalisierteTexte = new HashSet<>();
        for (AbstimmOption option : optionen) {
            if (option == null) {
                throw new IllegalArgumentException("Option darf nicht null sein.");
            }
            if (!normalisierteTexte.add(option.normalisierterText())) {
                throw new IllegalArgumentException("Doppelte Option gefunden: " + option.text());
            }
        }
        return List.copyOf(optionen);
    }

    /**
     * Erstellt eine neue Abstimmung mit einem Titel als String (wird in ein {@link Titel}-Objekt umgewandelt).
     *
     * @param id           die eindeutige ID der Abstimmung
     * @param erstelltVon  die NutzerId der Erstellerin / des Erstellers
     * @param titel        der Titel als String
     * @param beschreibung die Beschreibung (optional)
     * @param zeitraum     der Abstimmungszeitraum
     * @param optionen     die Liste der Antwortoptionen (mindestens zwei)
     * @return die erstellte Abstimmung
     */
    public static Abstimmung erstelle(AbstimmungId id,
                                      NutzerId erstelltVon,
                                      String titel,
                                      String beschreibung,
                                      Abstimmungszeitraum zeitraum,
                                      List<AbstimmOption> optionen) {
        return erstelle(id, erstelltVon, new Titel(titel), beschreibung, zeitraum, optionen);
    }

    public AbstimmungId getId() {
        return id;
    }

    public NutzerId getErstelltVon() {
        return erstelltVon;
    }

    public String getTitel() {
        return titel.wert();
    }

    public Titel getTitelObjekt() {
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

    /**
     * Prüft, ob die Abstimmung zum gegebenen Zeitpunkt aktiv läuft (im Abstimmungszeitraum liegt).
     *
     * @param zeitpunkt der zu prüfende Zeitpunkt
     * @return true, falls die Abstimmung läuft, sonst false
     */
    public boolean istLaufend(LocalDateTime zeitpunkt) {
        return zeitraum.enthaelt(zeitpunkt);
    }

    /**
     * Prüft, ob die Abstimmung zum gegebenen Zeitpunkt beendet ist (Frist abgelaufen).
     *
     * @param zeitpunkt der zu prüfende Zeitpunkt
     * @return true, falls die Frist abgelaufen ist, sonst false
     */
    public boolean istBeendet(LocalDateTime zeitpunkt) {
        return zeitraum.istVorbei(zeitpunkt);
    }

    /**
     * Prüft, ob die Abstimmung zum gegebenen Zeitpunkt noch nicht gestartet ist (in der Zukunft liegt).
     *
     * @param zeitpunkt der zu prüfende Zeitpunkt
     * @return true, falls der Beginn in der Zukunft liegt, sonst false
     */
    public boolean istNochNichtGestartet(LocalDateTime zeitpunkt) {
        return zeitraum.istInZukunft(zeitpunkt);
    }

    /**
     * Prüft, ob zum gegebenen Zeitpunkt eine Stimme abgegeben werden kann.
     * Fachlich gleichbedeutend mit {@link #istLaufend(LocalDateTime)}.
     *
     * @param zeitpunkt der zu prüfende Zeitpunkt
     * @return true, falls Stimmabgabe möglich ist, sonst false
     */
    public boolean kannAbgestimmtWerden(LocalDateTime zeitpunkt) {
        return istLaufend(zeitpunkt);
    }

    /**
     * Prüft, ob das Abstimmungsergebnis zum gegebenen Zeitpunkt eingesehen werden darf.
     * Fachlich erst nach Fristablauf ({@link #istBeendet(LocalDateTime)}) zulässig.
     *
     * @param zeitpunkt der zu prüfende Zeitpunkt
     * @return true, falls das Ergebnis einsehbar ist, sonst false
     */
    public boolean kannErgebnisEingesehenWerden(LocalDateTime zeitpunkt) {
        return istBeendet(zeitpunkt);
    }

    /**
     * Prüft, ob eine bestimmte AbstimmOption zur Abstimmung gehört (normalisierter Textvergleich).
     *
     * @param option die zu suchende Option
     * @return true, falls die Option existiert, sonst false
     */
    public boolean hatOption(AbstimmOption option) {
        if (option == null) {
            return false;
        }
        String gesucht = option.normalisierterText();
        return optionen.stream()
                .map(AbstimmOption::normalisierterText)
                .anyMatch(gesucht::equals);
    }

    /**
     * Prüft, ob eine AbstimmOption mit dem gegebenen Text zur Abstimmung gehört (normalisierter Vergleich).
     *
     * @param text der Text der gesuchten Option
     * @return true, falls eine entsprechende Option existiert, sonst false
     */
    public boolean hatOption(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return hatOption(new AbstimmOption(text));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Abstimmung that = (Abstimmung) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
