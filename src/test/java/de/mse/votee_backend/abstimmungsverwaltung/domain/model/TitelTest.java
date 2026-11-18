package de.mse.votee_backend.abstimmungsverwaltung.domain.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.Normalizer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests für das Value Object Titel.
 */
@DisplayName("Titel (Value Object)")
class TitelTest {

    @ParameterizedTest(name = "gültig: \"{0}\"")
    @ValueSource(strings = {
            "Neubau Hallenbad",
            "Straßenbahn in die Altstadt: Ja oder nein?",
            "Öffentliche Ü-Wagen-Stellplätze, Äpfel und Öl!",
            "Haushalt 2027.",
            "Grünfläche am Fluss"
    })
    @DisplayName("Happy Path: typische Titel werden übernommen")
    void typischeTitelWerdenUebernommen(String text) {
        Titel titel = new Titel(text);
        assertEquals(text, titel.wert());
    }

    @Test
    @DisplayName("Grenzfall: Randleerzeichen werden entfernt (getrimmt)")
    void randleerzeichenWerdenEntfernt() {
        Titel titel = new Titel("  Neubau Hallenbad  ");
        assertEquals("Neubau Hallenbad", titel.wert());
    }

    @Test
    @DisplayName("Grenzfall: Unicode NFD wird zu NFC normalisiert")
    void unicodeNfdWirdNormalisiert() {
        String nfd = Normalizer.normalize("Grünfläche", Normalizer.Form.NFD);
        Titel titel = new Titel(nfd);
        assertEquals("Grünfläche", titel.wert());
    }

    @ParameterizedTest(name = "gültig: \"{0}\"")
    @ValueSource(strings = {"Abc", "ÄÖÜ", "123"})
    @DisplayName("Grenzfall: genau 3 Zeichen (Untergrenze)")
    void genauDreiZeichenSindGueltig(String text) {
        assertDoesNotThrow(() -> new Titel(text));
    }

    @Test
    @DisplayName("Grenzfall: genau 100 Zeichen (Obergrenze)")
    void genauHundertZeichenSindGueltig() {
        assertDoesNotThrow(() -> new Titel("a".repeat(100)));
    }

    @ParameterizedTest(name = "ungültig: \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "\t",
            "\n",
            "Ab",
            "Titel mit @",
            "Titel; DROP TABLE",
            "<script>alert</script>",
            "---",
            "   A   "
    })
    @DisplayName("Negativ: ungültige Titel werfen IllegalArgumentException")
    void ungueltigeTitelWerfenException(String text) {
        assertThrows(IllegalArgumentException.class, () -> new Titel(text));
    }

    @Test
    @DisplayName("E6: geschütztes Leerzeichen (z. B. aus Word kopiert) gilt bewusst als ungültiges Zeichen")
    void geschuetztesLeerzeichenIstUngueltig() {
        assertThrows(IllegalArgumentException.class, () -> new Titel("Neubau Hallenbad\u00A0"));
    }

    @Test
    @DisplayName("Negativ: mehr als 100 Zeichen wirft IllegalArgumentException")
    void mehrAlsHundertZeichenWirftException() {
        assertThrows(IllegalArgumentException.class, () -> new Titel("a".repeat(101)));
    }

    @Test
    @DisplayName("Übung 5.2: unsichtbares Zero Width Space (U+200B) ist ein ungültiges Zeichen")
    void zeroWidthSpaceIstUngueltig() {
        assertThrows(IllegalArgumentException.class, () -> new Titel("Neubau\u200BHallenbad"));
    }

    @Test
    @DisplayName("Übung 5.2: Zeichen außerhalb der BMP zählen bei der Länge doppelt (UTF-16)")
    void zeichenAusserhalbBmpZaehlenDoppelt() {
        // "\uD801\uDC00" = U+10400, ein Buchstabe, der in Java aus zwei char besteht.
        // 50 dieser Buchstaben = 100 char (gültig), 51 = 102 char (zu lang).
        // Dokumentiert das aktuelle Verhalten; fachlich offen, ob "100 Zeichen" so gemeint ist.
        String buchstabe = "\uD801\uDC00";
        assertAll(
                () -> assertDoesNotThrow(() -> new Titel(buchstabe.repeat(50))),
                () -> assertThrows(IllegalArgumentException.class, () -> new Titel(buchstabe.repeat(51))));
    }
}
