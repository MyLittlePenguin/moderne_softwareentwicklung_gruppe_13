package de.mse.votee_backend.abstimmungsverwaltung.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests für das Value Object AbstimmungId (Identität der Aggregate Root Abstimmung).
 */
@DisplayName("AbstimmungId")
class AbstimmungIdTest {

    @Test
    @DisplayName("Happy Path: Wert wird übernommen")
    void wertWirdUebernommen() {
        assertEquals("abst-42", new AbstimmungId("abst-42").wert());
    }

    @Test
    @DisplayName("Factory: neu() erzeugt eine gültige, nicht-leere und eindeutige ID")
    void neuErzeugtEindeutigeId() {
        AbstimmungId id1 = AbstimmungId.neu();
        AbstimmungId id2 = AbstimmungId.neu();

        assertNotNull(id1.wert());
        assertFalse(id1.wert().isBlank());
        assertNotEquals(id1, id2);
    }

    @ParameterizedTest(name = "AbstimmungId(\"{0}\")")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Negativ: AbstimmungId ohne Wert")
    void abstimmungIdOhneWert(String wert) {
        assertThrows(IllegalArgumentException.class, () -> new AbstimmungId(wert));
    }
}
