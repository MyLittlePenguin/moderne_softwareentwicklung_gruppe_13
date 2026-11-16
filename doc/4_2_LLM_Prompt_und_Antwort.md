# Übung 4.2 – Anhang: LLM-Prompt und LLM-Antwort

**LLM:** Claude Pro (Opus 5.5) · **Datum:** 16.11.2026 · neuer Chat ohne Vorwissen

## Prompt

Du hilfst mir bei Test-Driven Development (TDD) in Java. Schreibe **nur Testfälle, keine Implementierung**.

**Projekt:** eVote „votee“, ein Online-Abstimmungssystem für Bürgerbefragungen. Java 27, JUnit 5, Domänenschicht ohne Framework (Domain-Driven Design).

**Zu testende Funktionalität:** Eine Verwaltungsangestellte (VA) erstellt eine Abstimmung (Bounded Context „Abstimmungsverwaltung“, Event „Abstimmung erstellt“).

**Domänenlogik:**
- `Abstimmung` ist Aggregate Root. Factory-Methode:
  `Abstimmung.erstelle(NutzerId erstelltVon, String titel, String beschreibung, Abstimmungszeitraum zeitraum, List<AbstimmOption> optionen)`
- `Abstimmungszeitraum` ist ein Value Object (Java record) mit `LocalDateTime beginn` und `LocalDateTime ende`. Die Frist der Abstimmung ist `ende`.
- `AbstimmOption` ist ein Value Object (record) mit `String text`.
- `NutzerId` ist ein Value Object (record) mit `String wert`.
- Bei Regelverletzung wird eine `IllegalArgumentException` geworfen.

**Fachregeln (Invarianten):**
1. Ersteller (NutzerId) ist gesetzt.
2. Titel ist nicht leer.
3. Beschreibung ist optional.
4. Das Ende des Zeitraums liegt nach dem Beginn.
5. Eine Abstimmung hat mindestens zwei Optionen; keine Option darf doppelt vorkommen; der Text einer Option ist nicht leer.

**Eingabevalidierung per Regular Expression** (Benutzereingaben kommen als String aus einem Formular, bevor daraus Domänenobjekte werden), Klasse `AbstimmungEingabeValidator`:
- `istGueltigerTitel(String)`: 3 bis 100 Zeichen, Buchstaben (inkl. Umlaute und ß), Ziffern, Leerzeichen und die Satzzeichen `- , . ? ! :`
- `istGueltigesDatum(String)`: Format `dd.MM.yyyy HH:mm`, z. B. `01.12.2026 08:00`

**Deine Aufgaben:**
1. Schlage Testfälle vor, gegliedert in **Happy-Path-Tests**, **Edge Cases (Grenzfälle)** und **Negativtests (ungültige Eingaben)**. Je Testfall: Name, Eingabe, erwartetes Ergebnis, kurze Begründung.
2. Generiere die Regular Expressions für `istGueltigerTitel` und `istGueltigesDatum` und erkläre jeden Teil.
3. Schreibe die JUnit-5-Testklassen `AbstimmungTest` und `AbstimmungEingabeValidatorTest`. Nutze `@ParameterizedTest` dort, wo es passt.

## Antwort des LLM (unverändert übernommen, Tabellen zur Lesbarkeit formatiert)

Ich schreibe die Testfälle, die Regex und beide JUnit-Testklassen. Danach prüfe ich, ob die Tests kompilieren und ob die Regex wirklich so trennt, wie die Tests es erwarten. Dafür nutze ich eine Wegwerf-Implementierung, die du nicht bekommst.

Ich habe zwei Testklassen geschrieben, AbstimmungTest.java und AbstimmungEingabeValidatorTest.java, mit 120 Testaufrufen. Sie decken alle fünf Fachregeln und beide Validierungen ab.

Wie geprüft: Maven Central ist in meiner Umgebung gesperrt. Deshalb liefen die Tests mit Java 21 gegen einen selbstgebauten Mini-Ersatz für JUnit und eine Wegwerf-Implementierung, die du nicht bekommst. Alle 120 Aufrufe waren grün. Mit echtem JUnit liefen sie noch nicht, sie verwenden aber nur Standard-API. Du brauchst die Abhängigkeit org.junit.jupiter:junit-jupiter, darin ist @ParameterizedTest schon enthalten.

#### 1. Testfälle

Erwartete Ergebnisse: ✅ = gültig, ❌ = IllegalArgumentException bzw. false.

**Abstimmung.erstelle(...)**

| Art | Testname | Eingabe | Ergebnis | Warum |
|---|---|---|---|---|
| Happy Path | erstelltAbstimmungMitAllenAngaben | alles gültig, Optionen Ja/Nein | ✅ alle Werte übernommen | Grundfunktion |
| Happy Path | fristIstEndeDesZeitraums | Zeitraum 01.12.–15.12. | frist() = Ende | Fachlich so definiert |
| Happy Path | registriertEreignisAbstimmungErstellt | gültig | genau 1 AbstimmungErstellt | Domain Event (Ereignis, das das Aggregat meldet) |
| Happy Path | behaeltReihenfolgeDerOptionen | Dafür/Dagegen/Enthaltung | gleiche Reihenfolge | Stimmzettel-Reihenfolge |
| Grenzfall | beschreibungIstOptional | null | ✅ | Regel 3 |
| Grenzfall | genauZweiOptionenSindErlaubt | 2 Optionen | ✅ | Untergrenze aus Regel 5 |
| Grenzfall | vieleOptionenSindErlaubt | 10 Optionen | ✅ | Es gibt keine Obergrenze |
| Grenzfall | endeEineMinuteNachBeginnIstErlaubt | Ende = Beginn + 1 min | ✅ | gerade noch gültig |
| Grenzfall | endeGleichBeginnIstNichtErlaubt | Ende = Beginn | ❌ | „nach“ ist strikt größer |
| Negativ | erstellerFehlt / nutzerIdOhneWert | null, "", "   " | ❌ | Regel 1 |
| Negativ | titelIstLeer | null, "", "   ", "\t", "\n" | ❌ | Regel 2 |
| Negativ | zeitraumFehlt, endeVorBeginn, beginnFehlt, endeFehlt | | ❌ | Regel 4 |
| Negativ | optionslisteFehlt, keineOption, nurEineOption | null, [], [Ja] | ❌ | Regel 5, Mindestanzahl |
| Negativ | doppelteOption | Ja, Nein, Ja | ❌ | Regel 5, keine Duplikate |
| Negativ | optionstextIstLeer, optionIstNull | Text leer bzw. null in der Liste | ❌ | Regel 5, Text nicht leer |

Eine wichtige Technik: Value Objects werden immer innerhalb des assertThrows-Lambdas gebaut, zum Beispiel `() -> erstelleMitZeitraum(new Abstimmungszeitraum(ENDE, BEGINN))`. Dadurch ist der Test grün, egal ob du die Prüfung später in den Record-Konstruktor oder in die Factory schreibst.

**AbstimmungEingabeValidator**

| Art | Titel-Beispiele | Datum-Beispiele |
|---|---|---|
| Happy Path ✅ | Straßenbahn in die Altstadt: Ja oder nein? | 01.12.2026 08:00 |
| Grenzfall ✅ | Abc (3 Zeichen), "a"×100, "Ä"×100, jedes erlaubte Satzzeichen einzeln | 01.01.2026 00:00, 31.12.2026 23:59, 29.02.2028 (Schaltjahr) |
| Grenzfall ❌ | Ab (2 Zeichen), "a"×101 | Tag 00/32, Monat 00/13, 24:00, 23:60 |
| Negativ ❌ | @, ;, &lt;script&gt;, (, €, ', %, _, Tab, \n, é, ñ, kyrillische Buchstaben, arabische Ziffern | 1.12.2026, ISO-Format, / als Trenner, T statt Leerzeichen, mit Sekunden, Leerzeichen am Rand, fünfstelliges Jahr, +2026 |

#### 2. Die Regular Expressions

Titel: `^(?=.*[A-Za-zÄÖÜäöüß0-9])[A-Za-zÄÖÜäöüß0-9 ,.?!:-]{3,100}$`

| Teil | Bedeutung |
|---|---|
| ^ und $ | Der ganze String muss passen, nicht nur ein Teil davon. |
| [ … ] | Zeichenklasse: genau ein Zeichen aus dieser Menge. |
| A-Za-z | Grundbuchstaben. é oder ñ gehören bewusst nicht dazu. |
| ÄÖÜäöüß | Die Umlaute und ß werden einzeln aufgezählt. |
| (Leerzeichen) | Nur das echte Leerzeichen. \s würde auch Tab und Zeilenumbruch erlauben. |
| ,.?!: | In einer Zeichenklasse ist . ein normaler Punkt und kein Platzhalter. |
| - am Ende | In der Mitte würde es einen Bereich bilden, wie bei a-z. Am Rand ist es ein echter Bindestrich. |
| {3,100} | Quantor: wie oft die Klasse davor vorkommen darf. |
| (?=…) | Lookahead: prüft eine Bedingung, ohne Zeichen zu „verbrauchen“. |

Der Lookahead ist meine Ergänzung. Ohne ihn würden "   " oder "---" als Titel durchgehen.

Datum: `^(0[1-9]|[12][0-9]|3[01])\.(0[1-9]|1[0-2])\.[0-9]{4} ([01][0-9]|2[0-3]):[0-5][0-9]$`

| Teil | Bedeutung | Erlaubt |
|---|---|---|
| (0[1-9]\|[12][0-9]\|3[01]) | Tag: drei Alternativen | 01–09, 10–29, 30–31 |
| \\. | Ein echter Punkt | . |
| (0[1-9]\|1[0-2]) | Monat | 01–12 |
| [0-9]{4} | Jahr, genau 4 Ziffern | 0000–9999 |
| (Leerzeichen) | Genau ein Leerzeichen | |
| ([01][0-9]\|2[0-3]) | Stunde | 00–23 |
| :[0-5][0-9] | Minute | 00–59 |

Ich schreibe [0-9] statt \d, denn mit dem Flag UNICODE_CHARACTER_CLASS würde \d auch arabische Ziffern akzeptieren.

Typischer Fehler: Eine Regex kann nicht rechnen. 31.04.2026 und 29.02.2027 passen durch die Schablone. Die Tests verlangen deshalb noch eine zweite Prüfung mit `DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT)`. Wichtig: uuuu statt yyyy, sonst lehnt Java mit STRICT jedes Datum ab.

#### 3. Eine Falle, auf die die Tests gestoßen sind

Meine erste Wegwerf-Implementierung prüfte die Liste mit `optionen.contains(null)`. Bei Listen aus `List.of(...)` wirft das eine NullPointerException. Prüfe besser mit `optionen.stream().anyMatch(Objects::isNull)`.

#### 4. Annahmen, die du klären solltest

- Eine leere Beschreibung ("" oder "   ") wird wie eine fehlende behandelt, beschreibung() liefert Optional.empty().
- Ja und ja gelten als doppelte Option (Groß-/Kleinschreibung und Rand-Leerzeichen ignoriert).
- Das Aggregat kopiert die Optionsliste, optionen() liefert eine unveränderliche Liste.
- Ein Titel nur aus Leer- oder Satzzeichen ist ungültig.
- Ein Datum, das es im Kalender nicht gibt, ist ungültig.

Die Tests legen außerdem diese Schnittstelle fest: erstelltVon(), titel(), beschreibung(), zeitraum(), optionen(), frist(), domainEvents() und die Event-Klasse AbstimmungErstellt. Der Validator ist eine Instanz im Paket …eingabe.

Noch offen, ohne Test: Darf der Beginn in der Vergangenheit liegen? Ist das große ẞ im Titel erlaubt? Werden Titel getrimmt gespeichert?

## Ursprünglich generierte Testklassen (vor unserer Überarbeitung)

<details>
<summary>AbstimmungTest.java (LLM-Version)</summary>

```java
package de.votee.abstimmungsverwaltung.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * Tests für das Erstellen einer Abstimmung (Aggregate Root, Event "Abstimmung erstellt").
 *
 * Wichtig für TDD: Value Objects, die bei ungültigen Werten evtl. schon im eigenen
 * Konstruktor eine Exception werfen, werden IMMER innerhalb des assertThrows-Lambdas
 * gebaut. So bleibt offen, ob die Prüfung im Record oder in der Factory steckt –
 * beide Implementierungen machen den Test grün.
 */
@DisplayName("Abstimmung.erstelle(...)")
class AbstimmungTest {

    // Feste Testdaten – keine LocalDateTime.now(), damit die Tests reproduzierbar sind
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
        return Abstimmung.erstelle(VERWALTUNGSANGESTELLTE, titel, BESCHREIBUNG, gueltigerZeitraum(), jaNein());
    }

    private static Abstimmung erstelleMitBeschreibung(String beschreibung) {
        return Abstimmung.erstelle(VERWALTUNGSANGESTELLTE, TITEL, beschreibung, gueltigerZeitraum(), jaNein());
    }

    private static Abstimmung erstelleMitZeitraum(Abstimmungszeitraum zeitraum) {
        return Abstimmung.erstelle(VERWALTUNGSANGESTELLTE, TITEL, BESCHREIBUNG, zeitraum, jaNein());
    }

    private static Abstimmung erstelleMitOptionen(List<AbstimmOption> optionen) {
        return Abstimmung.erstelle(VERWALTUNGSANGESTELLTE, TITEL, BESCHREIBUNG, gueltigerZeitraum(), optionen);
    }

    // =====================================================================
    @Nested
    @DisplayName("Happy Path")
    class HappyPath {

        @Test
        @DisplayName("übernimmt alle Angaben der Verwaltungsangestellten")
        void erstelltAbstimmungMitAllenAngaben() {
            Abstimmung abstimmung = Abstimmung.erstelle(
                    VERWALTUNGSANGESTELLTE, TITEL, BESCHREIBUNG, gueltigerZeitraum(), jaNein());

            assertAll(
                    () -> assertEquals(VERWALTUNGSANGESTELLTE, abstimmung.erstelltVon()),
                    () -> assertEquals(TITEL, abstimmung.titel()),
                    () -> assertEquals(BESCHREIBUNG, abstimmung.beschreibung().orElseThrow()),
                    () -> assertEquals(gueltigerZeitraum(), abstimmung.zeitraum()),
                    () -> assertEquals(jaNein(), abstimmung.optionen()));
        }

        @Test
        @DisplayName("Frist ist das Ende des Zeitraums")
        void fristIstEndeDesZeitraums() {
            Abstimmung abstimmung = erstelleMitZeitraum(gueltigerZeitraum());

            assertEquals(ENDE, abstimmung.frist());
        }

        @Test
        @DisplayName("registriert genau ein Ereignis 'AbstimmungErstellt'")
        void registriertEreignisAbstimmungErstellt() {
            Abstimmung abstimmung = erstelleMitTitel(TITEL);

            assertEquals(1, abstimmung.domainEvents().size());
            assertInstanceOf(AbstimmungErstellt.class, abstimmung.domainEvents().get(0));
        }

        @Test
        @DisplayName("behält die Reihenfolge der Optionen bei")
        void behaeltReihenfolgeDerOptionen() {
            List<AbstimmOption> eingabe = optionen("Dafür", "Dagegen", "Enthaltung");

            Abstimmung abstimmung = erstelleMitOptionen(eingabe);

            assertEquals(eingabe, abstimmung.optionen());
        }
    }

    // =====================================================================
    @Nested
    @DisplayName("Grenzfälle")
    class Grenzfaelle {

        @Test
        @DisplayName("Beschreibung ist optional (null)")
        void beschreibungIstOptional() {
            Abstimmung abstimmung = erstelleMitBeschreibung(null);

            assertTrue(abstimmung.beschreibung().isEmpty());
        }

        @Test
        @DisplayName("genau zwei Optionen sind erlaubt (Untergrenze)")
        void genauZweiOptionenSindErlaubt() {
            Abstimmung abstimmung = erstelleMitOptionen(optionen("Ja", "Nein"));

            assertEquals(2, abstimmung.optionen().size());
        }

        @Test
        @DisplayName("viele Optionen sind erlaubt (keine Obergrenze definiert)")
        void vieleOptionenSindErlaubt() {
            Abstimmung abstimmung = erstelleMitOptionen(
                    optionen("A", "B", "C", "D", "E", "F", "G", "H", "I", "J"));

            assertEquals(10, abstimmung.optionen().size());
        }

        @Test
        @DisplayName("Ende eine Minute nach Beginn ist erlaubt (knapp gültig)")
        void endeEineMinuteNachBeginnIstErlaubt() {
            Abstimmungszeitraum knapp = new Abstimmungszeitraum(BEGINN, BEGINN.plusMinutes(1));

            Abstimmung abstimmung = erstelleMitZeitraum(knapp);

            assertEquals(BEGINN.plusMinutes(1), abstimmung.frist());
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

        // --- Regel 1: Ersteller ist gesetzt -------------------------------

        @Test
        @DisplayName("Regel 1: Ersteller fehlt (null)")
        void erstellerFehlt() {
            assertThrows(IllegalArgumentException.class,
                    () -> Abstimmung.erstelle(null, TITEL, BESCHREIBUNG, gueltigerZeitraum(), jaNein()));
        }

        @ParameterizedTest(name = "NutzerId(\"{0}\")")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Regel 1: NutzerId ohne Wert")
        void nutzerIdOhneWert(String wert) {
            assertThrows(IllegalArgumentException.class,
                    () -> Abstimmung.erstelle(new NutzerId(wert), TITEL, BESCHREIBUNG,
                            gueltigerZeitraum(), jaNein()));
        }

        // --- Regel 2: Titel ist nicht leer --------------------------------

        @ParameterizedTest(name = "Titel = \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
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

        @Test
        @DisplayName("Regel 4: Beginn fehlt")
        void beginnFehlt() {
            assertThrows(IllegalArgumentException.class,
                    () -> erstelleMitZeitraum(new Abstimmungszeitraum(null, ENDE)));
        }

        @Test
        @DisplayName("Regel 4: Ende fehlt")
        void endeFehlt() {
            assertThrows(IllegalArgumentException.class,
                    () -> erstelleMitZeitraum(new Abstimmungszeitraum(BEGINN, null)));
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
        @ValueSource(strings = {"   "})
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
        @ValueSource(strings = {"", "   "})
        @DisplayName("Leere Beschreibung wird wie fehlende Beschreibung behandelt (leeres Optional)")
        void leereBeschreibungGiltAlsFehlend(String beschreibung) {
            Abstimmung abstimmung = erstelleMitBeschreibung(beschreibung);

            assertTrue(abstimmung.beschreibung().isEmpty());
        }

        @Test
        @DisplayName("Aggregat kopiert die Optionsliste (spätere Änderung von außen wirkt nicht)")
        void optionenWerdenDefensivKopiert() {
            List<AbstimmOption> veraenderbar = new ArrayList<>(jaNein());
            Abstimmung abstimmung = erstelleMitOptionen(veraenderbar);

            veraenderbar.add(new AbstimmOption("Vielleicht"));

            assertEquals(jaNein(), abstimmung.optionen());
        }

        @Test
        @DisplayName("optionen() liefert eine unveränderliche Liste")
        void optionenSindNachAussenUnveraenderlich() {
            Abstimmung abstimmung = erstelleMitOptionen(jaNein());

            assertThrows(UnsupportedOperationException.class,
                    () -> abstimmung.optionen().add(new AbstimmOption("Vielleicht")));
        }

        @ParameterizedTest(name = "\"Ja\" und \"{0}\" gelten als doppelt")
        @ValueSource(strings = {"ja", " Ja", "Ja ", "JA"})
        @DisplayName("Duplikate werden ohne Groß-/Kleinschreibung und Rand-Leerzeichen erkannt")
        void duplikateWerdenNormalisiertErkannt(String variante) {
            assertThrows(IllegalArgumentException.class,
                    () -> erstelleMitOptionen(List.of(new AbstimmOption("Ja"), new AbstimmOption(variante))));
        }
    }
}
```

</details>

<details>
<summary>AbstimmungEingabeValidatorTest.java (LLM-Version)</summary>

```java
package de.votee.abstimmungsverwaltung.eingabe;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests für die Formular-Eingabevalidierung (vor der Umwandlung in Domänenobjekte).
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
        @ValueSource(strings = {"Abc", "ÄÖÜ", "äöü", "ßßß", "123", "A-1"})
        @DisplayName("Grenzfall: genau 3 Zeichen (Untergrenze), Umlaute, ß, nur Ziffern")
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
                "Titel (neu)",
                "Preis 5 €",
                "Bürger's Wahl",
                "Zitat \"x\"",
                "50% Rabatt",
                "Pfad/zum/Titel",
                "Unter_strich"
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

        @ParameterizedTest(name = "ungültig: \"{0}\"")
        @ValueSource(strings = {"Café Central", "Señor", "Łódź-Fahrt", "Привет мир", "Wahl ١٢٣"})
        @DisplayName("Negativ: Buchstaben/Ziffern außerhalb von A–Z, 0–9, Umlauten und ß")
        void fremdeBuchstabenSindUngueltig(String titel) {
            assertFalse(validator.istGueltigerTitel(titel));
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
```

</details>
