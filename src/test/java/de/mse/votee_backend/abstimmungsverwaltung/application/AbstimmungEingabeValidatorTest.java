package de.mse.votee_backend.abstimmungsverwaltung.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.Normalizer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests für die Formular-Eingabevalidierung (vor der Umwandlung in Domänenobjekte).
 *
 * Grundlage: LLM-Vorschlag (Claude Opus 5.5, 16.11.2026), kritisch geprüft und angepasst,
 * siehe doc/4_Uebung.md, Abschnitt 2. Titelregel nach Team-Entscheidung gelockert:
 * alle Buchstaben (auch é, ñ), Ziffern 0-9, Leerzeichen und - , . ? ! : ( ) ' " / % €.
 */
@DisplayName("AbstimmungEingabeValidator")
class AbstimmungEingabeValidatorTest {

    private final AbstimmungEingabeValidator validator = new AbstimmungEingabeValidator();

    // =====================================================================
    @Nested
    @DisplayName("istGueltigerTitel(String)")
    class Titel {

        // ---------------- Happy Path ----------------

        @ParameterizedTest(name = "gültig: \"{0}\"")
        @ValueSource(strings = {
                "Neubau Hallenbad",
                "Straßenbahn in die Altstadt: Ja oder nein?",
                "Öffentliche Ü-Wagen-Stellplätze, Äpfel und Öl!",
                "Haushalt 2027.",
                "Grünfläche am Fluss"
        })
        @DisplayName("Happy Path: typische Titel")
        void typischeTitelSindGueltig(String titel) {
            assertTrue(validator.istGueltigerTitel(titel));
        }

        // ---------------- Grenzfälle ----------------

        @ParameterizedTest(name = "gültig: \"{0}\"")
        @ValueSource(strings = {"Abc", "ÄÖÜ", "123"})
        @DisplayName("Grenzfall: genau 3 Zeichen (Untergrenze), Umlaute, nur Ziffern")
        void dreiZeichenSindGueltig(String titel) {
            assertTrue(validator.istGueltigerTitel(titel));
        }

        @Test
        @DisplayName("Grenzfall: genau 100 Zeichen (Obergrenze) ist gültig")
        void hundertZeichenSindGueltig() {
            assertTrue(validator.istGueltigerTitel("a".repeat(100)));
        }

        @Test
        @DisplayName("Grenzfall: 100 Umlaute – ein Umlaut zählt als ein Zeichen")
        void hundertUmlauteSindGueltig() {
            assertTrue(validator.istGueltigerTitel("Ä".repeat(100)));
        }

        @Test
        @DisplayName("Grenzfall: 101 Zeichen ist ungültig")
        void hunderteinsZeichenSindUngueltig() {
            assertFalse(validator.istGueltigerTitel("a".repeat(101)));
        }

        @Test
        @DisplayName("Grenzfall: 2 Zeichen ist ungültig")
        void zweiZeichenSindUngueltig() {
            assertFalse(validator.istGueltigerTitel("Ab"));
        }

        @ParameterizedTest(name = "gültig: \"{0}\"")
        @ValueSource(strings = {"Wahl - 2027", "Ja, nein, egal", "Ende.", "Wer?", "Los!", "Frage: Park"})
        @DisplayName("Grenzfall: jedes erlaubte Satzzeichen einzeln")
        void jedesErlaubteSatzzeichenIstGueltig(String titel) {
            assertTrue(validator.istGueltigerTitel(titel));
        }

        // ---------------- Negativtests ----------------

        @ParameterizedTest(name = "ungültig: \"{0}\"")
        @NullAndEmptySource
        @DisplayName("Negativ: null und leerer String")
        void nullUndLeerSindUngueltig(String titel) {
            assertFalse(validator.istGueltigerTitel(titel));
        }

        @ParameterizedTest(name = "ungültig: \"{0}\"")
        @ValueSource(strings = {
                "Titel mit @",
                "Titel; DROP TABLE",
                "<script>alert</script>",
                "Unter_strich",
                "A & B",
                "x = y",
                "Pfad\\Datei"
        })
        @DisplayName("Negativ: nicht erlaubte Sonderzeichen")
        void nichtErlaubteZeichenSindUngueltig(String titel) {
            assertFalse(validator.istGueltigerTitel(titel));
        }

        @ParameterizedTest(name = "ungültig: \"{0}\"")
        @ValueSource(strings = {"Tab\tTitel", "Zeilen\numbruch", "Titel\n", "Titel\r\n"})
        @DisplayName("Negativ: Tabulator und Zeilenumbrüche sind kein 'Leerzeichen'")
        void steuerzeichenSindUngueltig(String titel) {
            assertFalse(validator.istGueltigerTitel(titel));
        }

        @ParameterizedTest(name = "gültig: \"{0}\"")
        @ValueSource(strings = {
                "Café Central",
                "Señor-Straße",
                "Titel (neu)",
                "Bürger's Wahl",
                "Zitat \"Park\"",
                "50% mehr Grün",
                "Preis 5 €",
                "Rad/Fußweg"
        })
        @DisplayName("Entscheidung: gelockerte Titelregel – diese Titel sind jetzt gültig")
        void gelockerteZeichenSindGueltig(String titel) {
            assertTrue(validator.istGueltigerTitel(titel));
        }

        @ParameterizedTest(name = "ungültig: \"{0}\"")
        @ValueSource(strings = {"Wahl ١٢٣", "Haushalt ²⁰²⁷"})
        @DisplayName("Negativ: Ziffern außerhalb von 0–9")
        void fremdeZiffernSindUngueltig(String titel) {
            assertFalse(validator.istGueltigerTitel(titel));
        }

        // ---------------- Ergänzt nach kritischer Prüfung ----------------

        @Test
        @DisplayName("Ergänzt: Umlaute in zerlegter Form (Unicode NFD, z. B. von macOS) sind gültig")
        void zerlegteUmlauteSindGueltig() {
            String zerlegt = Normalizer.normalize("Grünfläche am Fluss", Normalizer.Form.NFD);
            assertTrue(validator.istGueltigerTitel(zerlegt));
        }

        @ParameterizedTest(name = "ungültig: \"{0}\"")
        @ValueSource(strings = {"A  ", "  Ab  ", " A "})
        // Nur echte Leerzeichen am Rand werden ignoriert, Tab/Zeilenumbruch bleiben ungültig (siehe oben)
        @DisplayName("Ergänzt: Rand-Leerzeichen zählen nicht zur Mindestlänge")
        void randLeerzeichenZaehlenNicht(String titel) {
            assertFalse(validator.istGueltigerTitel(titel));
        }

        @Test
        @DisplayName("Ergänzt: Rand-Leerzeichen um einen gültigen Titel sind erlaubt")
        void randLeerzeichenUmGueltigenTitelSindErlaubt() {
            assertTrue(validator.istGueltigerTitel(" Neubau Hallenbad "));
        }

        // ---------------- Annahmen ----------------

        @ParameterizedTest(name = "ungültig: \"{0}\"")
        @ValueSource(strings = {"   ", "---", "?!.", " , "})
        @DisplayName("Annahme: Titel nur aus Leerzeichen/Satzzeichen ist ungültig (Regel 2 'nicht leer')")
        void titelOhneBuchstabeOderZifferIstUngueltig(String titel) {
            assertFalse(validator.istGueltigerTitel(titel));
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("istGueltigesDatum(String)")
    class Datum {

        // ---------------- Happy Path ----------------

        @ParameterizedTest(name = "gültig: \"{0}\"")
        @ValueSource(strings = {"01.12.2026 08:00", "15.06.2027 14:30"})
        @DisplayName("Happy Path: Format dd.MM.yyyy HH:mm")
        void typischeDatenSindGueltig(String datum) {
            assertTrue(validator.istGueltigesDatum(datum));
        }

        // ---------------- Grenzfälle ----------------

        @ParameterizedTest(name = "gültig: \"{0}\"")
        @ValueSource(strings = {
                "01.01.2026 00:00",   // kleinster Tag, Monat, Stunde, Minute
                "31.12.2026 23:59",   // größter Tag, Monat, Stunde, Minute
                "30.04.2026 10:00",   // letzter Tag eines 30-Tage-Monats
                "29.02.2028 12:00"    // Schalttag in einem Schaltjahr
        })
        @DisplayName("Grenzfall: kleinste/größte Werte und Monatsenden")
        void grenzwerteSindGueltig(String datum) {
            assertTrue(validator.istGueltigesDatum(datum));
        }

        @ParameterizedTest(name = "ungültig: \"{0}\"")
        @ValueSource(strings = {
                "00.12.2026 08:00",   // Tag 0
                "32.12.2026 08:00",   // Tag 32
                "01.00.2026 08:00",   // Monat 0
                "01.13.2026 08:00",   // Monat 13
                "01.12.2026 24:00",   // Stunde 24
                "01.12.2026 23:60"    // Minute 60
        })
        @DisplayName("Grenzfall: knapp außerhalb der Wertebereiche")
        void knappAusserhalbIstUngueltig(String datum) {
            assertFalse(validator.istGueltigesDatum(datum));
        }

        @ParameterizedTest(name = "ungültig: \"{0}\"")
        @ValueSource(strings = {
                "31.04.2026 10:00",   // April hat 30 Tage
                "30.02.2026 10:00",   // Februar hat nie 30 Tage
                "29.02.2027 10:00"    // 2027 ist kein Schaltjahr
        })
        @DisplayName("Annahme: Format korrekt, aber Datum existiert nicht im Kalender → ungültig")
        void nichtExistierendeDatenSindUngueltig(String datum) {
            // Eine Regex kennt keine Monatslängen und keine Schaltjahre. Diese Tests
            // erzwingen nach der Regex eine zweite, strikte Prüfung, z. B.
            // DateTimeFormatter "dd.MM.uuuu HH:mm" mit ResolverStyle.STRICT.
            assertFalse(validator.istGueltigesDatum(datum));
        }

        @Test
        @DisplayName("Ergänzt: Obergrenze Jahr 2099 ist gültig")
        void jahr2099IstGueltig() {
            assertTrue(validator.istGueltigesDatum("31.12.2099 23:59"));
        }

        @ParameterizedTest(name = "ungültig: \"{0}\"")
        @ValueSource(strings = {"01.12.0000 08:00", "31.12.1999 23:59", "01.01.2100 00:00"})
        @DisplayName("Ergänzt: Jahre außerhalb 2000–2099 sind unplausibel")
        void unplausiblesJahrIstUngueltig(String datum) {
            assertFalse(validator.istGueltigesDatum(datum));
        }

        // ---------------- Negativtests ----------------

        @ParameterizedTest(name = "ungültig: \"{0}\"")
        @NullAndEmptySource
        @DisplayName("Negativ: null und leerer String")
        void nullUndLeerSindUngueltig(String datum) {
            assertFalse(validator.istGueltigesDatum(datum));
        }

        @ParameterizedTest(name = "ungültig: \"{0}\"")
        @ValueSource(strings = {
                "1.12.2026 08:00",      // Tag einstellig
                "01.1.2026 08:00",      // Monat einstellig
                "01.12.26 08:00",       // Jahr zweistellig
                "01.12.2026 8:00",      // Stunde einstellig
                "01.12.2026 08:0",      // Minute einstellig
                "2026-12-01 08:00",     // ISO-Format
                "01/12/2026 08:00",     // falsches Trennzeichen
                "01.12.2026T08:00",     // T statt Leerzeichen
                "01.12.2026  08:00",    // zwei Leerzeichen
                "01.12.2026 08:00:00",  // mit Sekunden
                "01.12.20260 08:00",    // fünfstelliges Jahr
                "01.12.+2026 08:00",    // Jahr mit Vorzeichen
                "01.12.2026",           // ohne Uhrzeit
                "08:00",                // ohne Datum
                " 01.12.2026 08:00",    // führendes Leerzeichen
                "01.12.2026 08:00 ",    // nachgestelltes Leerzeichen
                "01.12.2026 08:00\n",   // nachgestellter Zeilenumbruch
                "ab.cd.efgh ij:kl",     // Buchstaben statt Ziffern
                "١٢.١٢.٢٠٢٦ ٠٨:٠٠"      // arabisch-indische Ziffern
        })
        @DisplayName("Negativ: falsches Format")
        void falschesFormatIstUngueltig(String datum) {
            assertFalse(validator.istGueltigesDatum(datum));
        }
    }
}
