package de.mse.votee_backend.abstimmungsverwaltung.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests für das Value Object NutzerId (Verweis auf die Verwaltungsangestellte, Regel 1).
 */
@DisplayName("NutzerId")
class NutzerIdTest {

    @Test
    @DisplayName("Happy Path: Wert wird übernommen")
    void wertWirdUebernommen() {
        assertEquals("va-4711", new NutzerId("va-4711").wert());
    }

    @ParameterizedTest(name = "NutzerId(\"{0}\")")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Negativ: NutzerId ohne Wert")
    void nutzerIdOhneWert(String wert) {
        assertThrows(IllegalArgumentException.class, () -> new NutzerId(wert));
    }
}
