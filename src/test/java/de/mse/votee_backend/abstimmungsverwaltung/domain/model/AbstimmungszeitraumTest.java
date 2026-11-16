package de.mse.votee_backend.abstimmungsverwaltung.domain.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
