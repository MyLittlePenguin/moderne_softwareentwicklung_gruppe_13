package de.mse.votee_backend.abstimmungsverwaltung.domain.model;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * Übung 4.4 (R7): Verletzte Fachregeln werfen eine fachliche Exception.
 * Sie erbt von IllegalArgumentException, deshalb bleiben die Tests aus 4.2 unverändert gültig.
 * Programmierfehler (z. B. null als Zeitpunkt bei einer Abfrage) bleiben IllegalArgumentException.
 */
@DisplayName("Fachliche Exception bei verletzten Abstimmungsregeln")
class AbstimmungsregelVerletztExceptionTest {

    private static final LocalDateTime BEGINN = LocalDateTime.of(2026, 12, 1, 8, 0);
    private static final LocalDateTime ENDE = LocalDateTime.of(2026, 12, 15, 18, 0);

    private static Abstimmung erstelle(String titel, List<AbstimmOption> optionen) {
        return Abstimmung.erstelle(new AbstimmungId("abst-1"), new NutzerId("va-1"), titel, "",
                new Abstimmungszeitraum(BEGINN, ENDE), optionen);
    }

    private static void assertRegelVerletzt(Executable aufruf) {
        assertThrows(AbstimmungsregelVerletztException.class, aufruf);
    }

    @Test
    @DisplayName("Regel 5: nur eine Option")
    void nurEineOption() {
        assertRegelVerletzt(() -> erstelle("Neubau Hallenbad", List.of(new AbstimmOption("Ja"))));
    }

    @Test
    @DisplayName("Regel 5: doppelte Option")
    void doppelteOption() {
        assertRegelVerletzt(() -> erstelle("Neubau Hallenbad",
                List.of(new AbstimmOption("Ja"), new AbstimmOption("ja"))));
    }

    @Test
    @DisplayName("Regel 5: leerer Optionstext")
    void leererOptionstext() {
        assertRegelVerletzt(() -> new AbstimmOption("  "));
    }

    @Test
    @DisplayName("Regel 2: Titel zu kurz")
    void titelZuKurz() {
        assertRegelVerletzt(() -> new Titel("Ab"));
    }

    @Test
    @DisplayName("Regel 4: Ende vor Beginn")
    void endeVorBeginn() {
        assertRegelVerletzt(() -> new Abstimmungszeitraum(ENDE, BEGINN));
    }

    @Test
    @DisplayName("Leere IDs verletzen die Regeln der Value Objects")
    void leereIds() {
        assertRegelVerletzt(() -> new AbstimmungId(" "));
        assertRegelVerletzt(() -> new NutzerId(""));
    }

    @Test
    @DisplayName("null als Zeitpunkt ist ein Programmierfehler, keine Fachregel")
    void nullZeitpunktBleibtIllegalArgumentException() {
        Abstimmungszeitraum zeitraum = new Abstimmungszeitraum(BEGINN, ENDE);

        assertThrowsExactly(IllegalArgumentException.class, () -> zeitraum.enthaelt(null));
    }
}
