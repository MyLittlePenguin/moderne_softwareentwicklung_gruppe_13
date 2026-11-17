package de.mse.votee_backend.abstimmungsverwaltung.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.Normalizer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests für den normalisierten Vergleichstext einer AbstimmOption (Übung 4.4, E1/R2).
 */
@DisplayName("AbstimmOption (Value Object)")
class AbstimmOptionTest {

    @ParameterizedTest(name = "\"{0}\" wird zu \"ja\"")
    @ValueSource(strings = { "Ja", "ja", "JA", " Ja", "Ja " })
    @DisplayName("Normalisierter Text ignoriert Groß-/Kleinschreibung und Rand-Leerzeichen")
    void normalisierterTextIgnoriertSchreibweise(String text) {
        assertEquals("ja", new AbstimmOption(text).normalisierterText());
    }

    @Test
    @DisplayName("E1: zerlegte Unicode-Form (NFD, z. B. von macOS) wird wie NFC behandelt")
    void normalisierterTextVereinheitlichtUnicode() {
        String nfd = Normalizer.normalize("Grün", Normalizer.Form.NFD);

        assertEquals(new AbstimmOption("Grün").normalisierterText(), new AbstimmOption(nfd).normalisierterText());
    }

    @Test
    @DisplayName("Der angezeigte Text bleibt unverändert")
    void textBleibtUnveraendert() {
        assertEquals(" Ja ", new AbstimmOption(" Ja ").text());
    }
}
