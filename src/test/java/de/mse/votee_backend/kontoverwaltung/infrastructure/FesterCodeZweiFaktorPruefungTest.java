package de.mse.votee_backend.kontoverwaltung.infrastructure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.mse.votee_backend.kontoverwaltung.application.ZweiFaktorPruefung;
import de.mse.votee_backend.kontoverwaltung.domain.model.EinmalCode;

/**
 * Tests für den vorläufigen 2FA-Stub, der den festen 5-stelligen Code {@code 00000} akzeptiert.
 */
@DisplayName("FesterCodeZweiFaktorPruefung")
class FesterCodeZweiFaktorPruefungTest {

    private final ZweiFaktorPruefung pruefung = new FesterCodeZweiFaktorPruefung();

    @Test
    @DisplayName("Akzeptiert exakt 00000 und lehnt andere 5-stellige Codes ab")
    void akzeptiertNurNullenCode() {
        assertTrue(pruefung.pruefeCode("admin-1", new EinmalCode("00000")));
        assertFalse(pruefung.pruefeCode("admin-1", new EinmalCode("00001")));
        assertFalse(pruefung.pruefeCode("admin-1", new EinmalCode("12345")));
        assertFalse(pruefung.pruefeCode("admin-1", null));
        assertFalse(pruefung.pruefeCode(null, new EinmalCode("00000")));
    }
}
