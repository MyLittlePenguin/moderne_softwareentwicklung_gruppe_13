package de.mse.votee_backend.kontoverwaltung.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests für das Value Object {@link EinmalCode} (2FA-Einmalcode mit exakt 5 Ziffern).
 */
@DisplayName("EinmalCode")
class EinmalCodeTest {

    @ParameterizedTest(name = "gültig: \"{0}\"")
    @ValueSource(strings = {"00000", "12345", "99999", "04207"})
    @DisplayName("Happy Path: exakt 5 Ziffern sind gültig")
    void fuenfZiffernSindGueltig(String code) {
        EinmalCode einmalCode = new EinmalCode(code);
        assertEquals(code, einmalCode.wert());
    }

    @ParameterizedTest(name = "ungültig: \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {
            "     ",
            "0000",      // 4 Stellen
            "000000",    // 6 Stellen
            "1234a",     // Buchstabe
            " 00000",    // führendes Leerzeichen
            "00000 ",    // nachgestelltes Leerzeichen
            "00-00",     // Sonderzeichen
            "١٢٣٤٥"      // arabisch-indische Ziffern
    })
    @DisplayName("Negativ: alles außer exakt 5 ASCII-Ziffern wird abgelehnt")
    void ungueltigeCodesWerdenAbgelehnt(String code) {
        assertThrows(IllegalArgumentException.class, () -> new EinmalCode(code));
    }
}
