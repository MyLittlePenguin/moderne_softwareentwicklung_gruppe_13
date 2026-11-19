package de.mse.votee_backend.abstimmungsverwaltung.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests für die fachliche Berechtigungs-Exception.
 */
@DisplayName("KeineBerechtigungException")
class KeineBerechtigungExceptionTest {

    @Test
    @DisplayName("Übernimmt Meldung und ist klar von Validierungsfehlern getrennt")
    void uebernimmtMeldungUndIstKeineIllegalArgumentException() {
        KeineBerechtigungException exception =
                new KeineBerechtigungException("Nur Administrator:innen dürfen Abstimmungen erstellen.");

        assertEquals("Nur Administrator:innen dürfen Abstimmungen erstellen.", exception.getMessage());
        assertInstanceOf(RuntimeException.class, exception);
        assertFalse(IllegalArgumentException.class.isInstance(exception));
    }
}
