package de.mse.votee_backend.abstimmungsverwaltung.domain.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests für das Value Object Abstimmungszeitraum (Regel 4: Ende liegt nach Beginn).
 * Aus AbstimmungTest herausgelöst, weil sie nur den Zeitraum betreffen.
 */
@DisplayName("Abstimmungszeitraum")
class AbstimmungszeitraumTest {

    private static final LocalDateTime BEGINN = LocalDateTime.of(2026, 12, 1, 8, 0);
    private static final LocalDateTime ENDE = LocalDateTime.of(2026, 12, 15, 18, 0);

    @Test
    @DisplayName("Happy Path: Ende nach Beginn")
    void endeNachBeginnIstGueltig() {
        assertDoesNotThrow(() -> new Abstimmungszeitraum(BEGINN, ENDE));
    }

    @Test
    @DisplayName("Negativ: Beginn fehlt")
    void beginnFehlt() {
        assertThrows(IllegalArgumentException.class, () -> new Abstimmungszeitraum(null, ENDE));
    }

    @Test
    @DisplayName("Negativ: Ende fehlt")
    void endeFehlt() {
        assertThrows(IllegalArgumentException.class, () -> new Abstimmungszeitraum(BEGINN, null));
    }

    @Test
    @DisplayName("Fachliches Verhalten: enthaelt(zeitpunkt)")
    void enthaeltPrueftGrenzwerte() {
        Abstimmungszeitraum zeitraum = new Abstimmungszeitraum(BEGINN, ENDE);

        assertTrue(zeitraum.enthaelt(BEGINN));
        assertTrue(zeitraum.enthaelt(BEGINN.plusDays(1)));
        assertTrue(zeitraum.enthaelt(ENDE));
        assertFalse(zeitraum.enthaelt(BEGINN.minusMinutes(1)));
        assertFalse(zeitraum.enthaelt(ENDE.plusMinutes(1)));
        assertThrows(IllegalArgumentException.class, () -> zeitraum.enthaelt(null));
    }

    @Test
    @DisplayName("Fachliches Verhalten: istVorbei(zeitpunkt)")
    void istVorbeiPrueftEnde() {
        Abstimmungszeitraum zeitraum = new Abstimmungszeitraum(BEGINN, ENDE);

        assertFalse(zeitraum.istVorbei(BEGINN));
        assertFalse(zeitraum.istVorbei(ENDE));
        assertTrue(zeitraum.istVorbei(ENDE.plusMinutes(1)));
        assertThrows(IllegalArgumentException.class, () -> zeitraum.istVorbei(null));
    }

    @Test
    @DisplayName("Fachliches Verhalten: istInZukunft(zeitpunkt)")
    void istInZukunftPrueftBeginn() {
        Abstimmungszeitraum zeitraum = new Abstimmungszeitraum(BEGINN, ENDE);

        assertTrue(zeitraum.istInZukunft(BEGINN.minusMinutes(1)));
        assertFalse(zeitraum.istInZukunft(BEGINN));
        assertFalse(zeitraum.istInZukunft(ENDE));
        assertThrows(IllegalArgumentException.class, () -> zeitraum.istInZukunft(null));
    }

    @Test
    @DisplayName("Fachregel: Beginn darf bei Erstellung nicht in der Vergangenheit liegen")
    void pruefeBeginnNichtInVergangenheit() {
        Abstimmungszeitraum zeitraum = new Abstimmungszeitraum(BEGINN, ENDE);

        assertDoesNotThrow(() -> zeitraum.pruefeBeginnNichtInVergangenheit(BEGINN.minusMinutes(1)));
        assertDoesNotThrow(() -> zeitraum.pruefeBeginnNichtInVergangenheit(BEGINN));
        assertThrows(AbstimmungsregelVerletztException.class,
                () -> zeitraum.pruefeBeginnNichtInVergangenheit(BEGINN.plusSeconds(1)));
        assertThrows(AbstimmungsregelVerletztException.class,
                () -> zeitraum.pruefeBeginnNichtInVergangenheit(BEGINN.plusMinutes(1)));
        assertThrowsExactly(IllegalArgumentException.class,
                () -> zeitraum.pruefeBeginnNichtInVergangenheit(null));
    }
}
