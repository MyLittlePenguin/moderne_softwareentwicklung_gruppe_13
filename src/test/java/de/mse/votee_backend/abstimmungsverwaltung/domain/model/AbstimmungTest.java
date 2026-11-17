package de.mse.votee_backend.abstimmungsverwaltung.domain.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests für das Erstellen einer Abstimmung (Aggregate Root, Event "Abstimmung
 * erstellt").
 *
 * Grundlage: LLM-Vorschlag (Claude Opus 5.5, 16.11.2026), kritisch geprüft und
 * angepasst,
 * siehe doc/4_Uebung.md, Abschnitt 2.
 *
 * Wichtig für TDD: Value Objects, die bei ungültigen Werten evtl. schon im
 * eigenen
 * Konstruktor eine Exception werfen, werden IMMER innerhalb des
 * assertThrows-Lambdas
 * gebaut. So bleibt offen, ob die Prüfung im Record oder in der Factory steckt
 * –
 * beide Implementierungen machen den Test grün.
 */
@DisplayName("Abstimmung.erstelle(...)")
class AbstimmungTest {

    // Feste Testdaten – keine LocalDateTime.now(), damit die Tests reproduzierbar
    // sind
    private static final AbstimmungId ABSTIMMUNG_ID = new AbstimmungId("abst-42");
    private static final NutzerId VERWALTUNGSANGESTELLTE = new NutzerId("va-4711");
    private static final String TITEL = "Neubau Hallenbad";
    private static final String BESCHREIBUNG = "Soll die Stadt ein neues Hallenbad bauen?";
    private static final LocalDateTime BEGINN = LocalDateTime.of(2026, 12, 1, 8, 0);
    private static final LocalDateTime ENDE = LocalDateTime.of(2026, 12, 15, 18, 0);

    private static Abstimmungszeitraum gueltigerZeitraum() {
        return new Abstimmungszeitraum(BEGINN, ENDE);
    }

    private static List<AbstimmOption> optionen(String... texte) {
        return Arrays.stream(texte).map(AbstimmOption::new).toList();
    }

    private static List<AbstimmOption> jaNein() {
        return optionen("Ja", "Nein");
    }

    // Hilfsmethoden: alles gültig außer dem einen Parameter, der getestet wird
    private static Abstimmung erstelleMitTitel(String titel) {
        return Abstimmung.erstelle(ABSTIMMUNG_ID, VERWALTUNGSANGESTELLTE, titel, BESCHREIBUNG,
                gueltigerZeitraum(), jaNein());
    }

    private static Abstimmung erstelleMitBeschreibung(String beschreibung) {
        return Abstimmung.erstelle(ABSTIMMUNG_ID, VERWALTUNGSANGESTELLTE, TITEL, beschreibung,
                gueltigerZeitraum(), jaNein());
    }

    private static Abstimmung erstelleMitZeitraum(Abstimmungszeitraum zeitraum) {
        return Abstimmung.erstelle(ABSTIMMUNG_ID, VERWALTUNGSANGESTELLTE, TITEL, BESCHREIBUNG,
                zeitraum, jaNein());
    }

    private static Abstimmung erstelleMitOptionen(List<AbstimmOption> optionen) {
        return Abstimmung.erstelle(ABSTIMMUNG_ID, VERWALTUNGSANGESTELLTE, TITEL, BESCHREIBUNG,
                gueltigerZeitraum(), optionen);
    }

    // =====================================================================
    @Nested
    @DisplayName("Happy Path")
    class HappyPath {

        @Test
        @DisplayName("übernimmt alle Angaben der Verwaltungsangestellten")
        void erstelltAbstimmungMitAllenAngaben() {
            Abstimmung abstimmung = Abstimmung.erstelle(
                    ABSTIMMUNG_ID, VERWALTUNGSANGESTELLTE, TITEL, BESCHREIBUNG, gueltigerZeitraum(), jaNein());

            assertAll(
                    () -> assertEquals(ABSTIMMUNG_ID, abstimmung.getId()),
                    () -> assertEquals(VERWALTUNGSANGESTELLTE, abstimmung.getErstelltVon()),
                    () -> assertEquals(TITEL, abstimmung.getTitel()),
                    () -> assertEquals(BESCHREIBUNG, abstimmung.getBeschreibung()),
                    () -> assertEquals(gueltigerZeitraum(), abstimmung.getZeitraum()),
                    () -> assertEquals(jaNein(), abstimmung.getOptionen()));
        }

        @Test
        @DisplayName("Frist ist das Ende des Zeitraums")
        void fristIstEndeDesZeitraums() {
            Abstimmung abstimmung = erstelleMitZeitraum(gueltigerZeitraum());

            assertEquals(ENDE, abstimmung.getFrist());
        }

        @Test
        @DisplayName("behält die Reihenfolge der Optionen bei")
        void behaeltReihenfolgeDerOptionen() {
            List<AbstimmOption> eingabe = optionen("Dafür", "Dagegen", "Enthaltung");

            Abstimmung abstimmung = erstelleMitOptionen(eingabe);

            assertEquals(eingabe, abstimmung.getOptionen());
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Grenzfälle")
    class Grenzfaelle {

        @Test
        @DisplayName("Beschreibung ist optional (null wird zu leerem Text)")
        void beschreibungIstOptional() {
            Abstimmung abstimmung = erstelleMitBeschreibung(null);

            assertEquals("", abstimmung.getBeschreibung());
        }

        @Test
        @DisplayName("viele Optionen sind erlaubt (keine Obergrenze definiert)")
        void vieleOptionenSindErlaubt() {
            Abstimmung abstimmung = erstelleMitOptionen(
                    optionen("A", "B", "C", "D", "E", "F", "G", "H", "I", "J"));

            assertEquals(10, abstimmung.getOptionen().size());
        }

        @Test
        @DisplayName("Ende eine Minute nach Beginn ist erlaubt (knapp gültig)")
        void endeEineMinuteNachBeginnIstErlaubt() {
            Abstimmungszeitraum knapp = new Abstimmungszeitraum(BEGINN, BEGINN.plusMinutes(1));

            Abstimmung abstimmung = erstelleMitZeitraum(knapp);

            assertEquals(BEGINN.plusMinutes(1), abstimmung.getFrist());
        }

        @Test
        @DisplayName("Ende gleich Beginn ist NICHT erlaubt (knapp ungültig)")
        void endeGleichBeginnIstNichtErlaubt() {
            assertThrows(IllegalArgumentException.class,
                    () -> erstelleMitZeitraum(new Abstimmungszeitraum(BEGINN, BEGINN)));
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Negativtests (Regel verletzt → IllegalArgumentException)")
    class Negativtests {

        // --- Regel 0: AbstimmungId ist gesetzt ----------------------------

        @Test
        @DisplayName("Regel 0: AbstimmungId fehlt (null)")
        void abstimmungIdFehlt() {
            assertThrows(IllegalArgumentException.class,
                    () -> Abstimmung.erstelle(null, VERWALTUNGSANGESTELLTE, TITEL, BESCHREIBUNG,
                            gueltigerZeitraum(), jaNein()));
        }

        // --- Regel 1: Ersteller ist gesetzt -------------------------------

        @Test
        @DisplayName("Regel 1: Ersteller fehlt (null)")
        void erstellerFehlt() {
            assertThrows(IllegalArgumentException.class,
                    () -> Abstimmung.erstelle(ABSTIMMUNG_ID, null, TITEL, BESCHREIBUNG,
                            gueltigerZeitraum(), jaNein()));
        }

        // --- Regel 2: Titel ist nicht leer --------------------------------

        @ParameterizedTest(name = "Titel = \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = { "   ", "\t", "\n" })
        @DisplayName("Regel 2: Titel ist leer oder besteht nur aus Leerraum")
        void titelIstLeer(String titel) {
            assertThrows(IllegalArgumentException.class, () -> erstelleMitTitel(titel));
        }

        // --- Regel 4: Ende liegt nach Beginn ------------------------------

        @Test
        @DisplayName("Regel 4: Zeitraum fehlt (null)")
        void zeitraumFehlt() {
            assertThrows(IllegalArgumentException.class, () -> erstelleMitZeitraum(null));
        }

        @Test
        @DisplayName("Regel 4: Ende liegt vor dem Beginn")
        void endeVorBeginn() {
            assertThrows(IllegalArgumentException.class,
                    () -> erstelleMitZeitraum(new Abstimmungszeitraum(ENDE, BEGINN)));
        }

        // --- Regel 5: Optionen --------------------------------------------

        @Test
        @DisplayName("Regel 5: Optionsliste fehlt (null)")
        void optionslisteFehlt() {
            assertThrows(IllegalArgumentException.class, () -> erstelleMitOptionen(null));
        }

        @Test
        @DisplayName("Regel 5: keine Option")
        void keineOption() {
            assertThrows(IllegalArgumentException.class, () -> erstelleMitOptionen(List.of()));
        }

        @Test
        @DisplayName("Regel 5: nur eine Option (eine unter der Untergrenze)")
        void nurEineOption() {
            assertThrows(IllegalArgumentException.class, () -> erstelleMitOptionen(optionen("Ja")));
        }

        @Test
        @DisplayName("Regel 5: Option kommt doppelt vor")
        void doppelteOption() {
            assertThrows(IllegalArgumentException.class,
                    () -> erstelleMitOptionen(optionen("Ja", "Nein", "Ja")));
        }

        @ParameterizedTest(name = "Optionstext = \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = { "   " })
        @DisplayName("Regel 5: Text einer Option ist leer")
        void optionstextIstLeer(String text) {
            assertThrows(IllegalArgumentException.class,
                    () -> erstelleMitOptionen(List.of(new AbstimmOption("Ja"), new AbstimmOption(text))));
        }

        @Test
        @DisplayName("Regel 5: Liste enthält null statt einer Option")
        void optionIstNull() {
            List<AbstimmOption> mitNull = Arrays.asList(new AbstimmOption("Ja"), null);

            assertThrows(IllegalArgumentException.class, () -> erstelleMitOptionen(mitNull));
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Annahmen – mit dem Fachbereich klären, bevor implementiert wird")
    class Annahmen {

        @ParameterizedTest(name = "Beschreibung = \"{0}\" gilt als 'keine Beschreibung'")
        @ValueSource(strings = { "", "   " })
        @DisplayName("Leere Beschreibung wird wie fehlende Beschreibung behandelt (leerer Text)")
        void leereBeschreibungGiltAlsFehlend(String beschreibung) {
            Abstimmung abstimmung = erstelleMitBeschreibung(beschreibung);

            assertEquals("", abstimmung.getBeschreibung());
        }

        @Test
        @DisplayName("Aggregat kopiert die Optionsliste (spätere Änderung von außen wirkt nicht)")
        void optionenWerdenDefensivKopiert() {
            List<AbstimmOption> veraenderbar = new ArrayList<>(jaNein());
            Abstimmung abstimmung = erstelleMitOptionen(veraenderbar);

            veraenderbar.add(new AbstimmOption("Vielleicht"));

            assertEquals(jaNein(), abstimmung.getOptionen());
        }

        @Test
        @DisplayName("getOptionen() liefert eine unveränderliche Liste")
        void optionenSindNachAussenUnveraenderlich() {
            Abstimmung abstimmung = erstelleMitOptionen(jaNein());

            assertThrows(UnsupportedOperationException.class,
                    () -> abstimmung.getOptionen().add(new AbstimmOption("Vielleicht")));
        }

        @ParameterizedTest(name = "\"Ja\" und \"{0}\" gelten als doppelt")
        @ValueSource(strings = { "ja", " Ja", "Ja ", "JA" })
        @DisplayName("Duplikate werden ohne Groß-/Kleinschreibung und Rand-Leerzeichen erkannt")
        void duplikateWerdenNormalisiertErkannt(String variante) {
            assertThrows(IllegalArgumentException.class,
                    () -> erstelleMitOptionen(List.of(new AbstimmOption("Ja"), new AbstimmOption(variante))));
        }

        @Test
        @DisplayName("E1: Duplikate werden auch in zerlegter Unicode-Form (NFD) erkannt")
        void duplikateInNfdFormWerdenErkannt() {
            String nfd = Normalizer.normalize("Grün", Normalizer.Form.NFD);

            assertThrows(IllegalArgumentException.class,
                    () -> erstelleMitOptionen(List.of(new AbstimmOption("Grün"), new AbstimmOption(nfd))));
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Entitäts-Gleichheit (Entity Identity)")
    class EntitaetsGleichheit {

        @Test
        @DisplayName("gleiche AbstimmungId bedeutet Gleichheit (unabhängig von Attributen)")
        void gleicheIdBedeutetGleichheit() {
            Abstimmung a1 = Abstimmung.erstelle(
                    ABSTIMMUNG_ID, VERWALTUNGSANGESTELLTE, "Titel 1", BESCHREIBUNG,
                    gueltigerZeitraum(), jaNein());
            Abstimmung a2 = Abstimmung.erstelle(
                    ABSTIMMUNG_ID, new NutzerId("va-9999"), "Anderer Titel", "",
                    gueltigerZeitraum(), optionen("A", "B"));

            assertAll(
                    () -> assertEquals(a1, a2),
                    () -> assertEquals(a1.hashCode(), a2.hashCode()));
        }

        @Test
        @DisplayName("unterschiedliche AbstimmungId bedeutet Ungleichheit (selbst bei identischen Attributen)")
        void unterschiedlicheIdBedeutetUngleichheit() {
            Abstimmung a1 = Abstimmung.erstelle(
                    new AbstimmungId("abst-1"), VERWALTUNGSANGESTELLTE, TITEL, BESCHREIBUNG,
                    gueltigerZeitraum(), jaNein());
            Abstimmung a2 = Abstimmung.erstelle(
                    new AbstimmungId("abst-2"), VERWALTUNGSANGESTELLTE, TITEL, BESCHREIBUNG,
                    gueltigerZeitraum(), jaNein());

            assertNotEquals(a1, a2);
        }

        @Test
        @DisplayName("Vergleich mit null oder anderem Typ liefert false")
        void vergleichMitNullOderAnderemTyp() {
            Abstimmung a = erstelleMitTitel(TITEL);

            assertAll(
                    () -> assertNotEquals(null, a),
                    () -> assertNotEquals("keine Abstimmung", a));
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Fachliches Verhalten (Domain Behavior)")
    class FachlichesVerhalten {

        @Test
        @DisplayName("istLaufend prüft, ob die Abstimmung im Zeitraum aktiv ist")
        void istLaufendPrueftAktivitaet() {
            Abstimmung abstimmung = erstelleMitZeitraum(gueltigerZeitraum());

            assertAll(
                    () -> assertFalse(abstimmung.istLaufend(BEGINN.minusMinutes(1))),
                    () -> assertTrue(abstimmung.istLaufend(BEGINN)),
                    () -> assertTrue(abstimmung.istLaufend(BEGINN.plusDays(1))),
                    () -> assertTrue(abstimmung.istLaufend(ENDE)),
                    () -> assertFalse(abstimmung.istLaufend(ENDE.plusMinutes(1))),
                    () -> assertThrows(IllegalArgumentException.class, () -> abstimmung.istLaufend(null)));
        }

        @Test
        @DisplayName("istBeendet prüft, ob die Frist abgelaufen ist")
        void istBeendetPrueftFristablauf() {
            Abstimmung abstimmung = erstelleMitZeitraum(gueltigerZeitraum());

            assertAll(
                    () -> assertFalse(abstimmung.istBeendet(BEGINN.minusDays(1))),
                    () -> assertFalse(abstimmung.istBeendet(BEGINN)),
                    () -> assertFalse(abstimmung.istBeendet(ENDE)),
                    () -> assertTrue(abstimmung.istBeendet(ENDE.plusMinutes(1))),
                    () -> assertThrows(IllegalArgumentException.class, () -> abstimmung.istBeendet(null)));
        }

        @Test
        @DisplayName("istNochNichtGestartet prüft, ob die Abstimmung in der Zukunft liegt")
        void istNochNichtGestartetPrueftBeginn() {
            Abstimmung abstimmung = erstelleMitZeitraum(gueltigerZeitraum());

            assertAll(
                    () -> assertTrue(abstimmung.istNochNichtGestartet(BEGINN.minusMinutes(1))),
                    () -> assertFalse(abstimmung.istNochNichtGestartet(BEGINN)),
                    () -> assertFalse(abstimmung.istNochNichtGestartet(ENDE)),
                    () -> assertThrows(IllegalArgumentException.class, () -> abstimmung.istNochNichtGestartet(null)));
        }

        @Test
        @DisplayName("kannAbgestimmtWerden ist nur während der Laufzeit true")
        void kannAbgestimmtWerdenSemantik() {
            Abstimmung abstimmung = erstelleMitZeitraum(gueltigerZeitraum());

            assertAll(
                    () -> assertFalse(abstimmung.kannAbgestimmtWerden(BEGINN.minusMinutes(1))),
                    () -> assertTrue(abstimmung.kannAbgestimmtWerden(BEGINN)),
                    () -> assertTrue(abstimmung.kannAbgestimmtWerden(ENDE)),
                    () -> assertFalse(abstimmung.kannAbgestimmtWerden(ENDE.plusMinutes(1))));
        }

        @Test
        @DisplayName("kannErgebnisEingesehenWerden ist erst nach Fristablauf true")
        void kannErgebnisEingesehenWerdenSemantik() {
            Abstimmung abstimmung = erstelleMitZeitraum(gueltigerZeitraum());

            assertAll(
                    () -> assertFalse(abstimmung.kannErgebnisEingesehenWerden(BEGINN)),
                    () -> assertFalse(abstimmung.kannErgebnisEingesehenWerden(ENDE)),
                    () -> assertTrue(abstimmung.kannErgebnisEingesehenWerden(ENDE.plusMinutes(1))));
        }

        @Test
        @DisplayName("hatOption prüft Existenz einer Option anhand von Objekt oder Text normalisiert")
        void hatOptionPrueftOptionen() {
            Abstimmung abstimmung = erstelleMitOptionen(optionen("Ja", "Nein"));

            assertAll(
                    () -> assertTrue(abstimmung.hatOption(new AbstimmOption("Ja"))),
                    () -> assertTrue(abstimmung.hatOption(new AbstimmOption(" ja "))),
                    () -> assertTrue(abstimmung.hatOption("Ja")),
                    () -> assertTrue(abstimmung.hatOption(" ja ")),
                    () -> assertTrue(abstimmung.hatOption("JA")),
                    () -> assertFalse(abstimmung.hatOption(new AbstimmOption("Vielleicht"))),
                    () -> assertFalse(abstimmung.hatOption("Vielleicht")),
                    () -> assertFalse(abstimmung.hatOption((AbstimmOption) null)),
                    () -> assertFalse(abstimmung.hatOption((String) null)),
                    () -> assertFalse(abstimmung.hatOption("   ")));
        }

        @Test
        @DisplayName("E1: hatOption findet eine Option auch in zerlegter Unicode-Form (NFD)")
        void hatOptionErkenntNfdForm() {
            Abstimmung abstimmung = erstelleMitOptionen(optionen("Grün", "Rot"));
            String nfd = Normalizer.normalize("Grün", Normalizer.Form.NFD);

            assertTrue(abstimmung.hatOption(nfd));
        }
    }
}
