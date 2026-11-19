package de.mse.votee_backend.kontoverwaltung.infrastructure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.mse.votee_backend.kontoverwaltung.application.PasswortHasher;

/**
 * Tests für den PBKDF2-Passwort-Hasher.
 */
@DisplayName("Pbkdf2PasswortHasher")
class Pbkdf2PasswortHasherTest {

    private final PasswortHasher hasher = new Pbkdf2PasswortHasher();

    @Test
    @DisplayName("Erzeugt unterschiedliche gesalzene Hashes und verifiziert das korrekte Passwort")
    void hashtMitZufaelligemSaltUndVerifiziert() {
        String hash1 = hasher.hashe("GeheimesPasswort!2027");
        String hash2 = hasher.hashe("GeheimesPasswort!2027");

        assertNotEquals(hash1, hash2);
        assertTrue(hasher.pruefe("GeheimesPasswort!2027", hash1));
        assertTrue(hasher.pruefe("GeheimesPasswort!2027", hash2));
        assertFalse(hasher.pruefe("FalschesPasswort", hash1));
    }

    @Test
    @DisplayName("Lehnt null, leere Eingaben oder beschädigte Hashes sicher ab")
    void lehntUngueltigeEingabenAb() {
        String hash = hasher.hashe("passwort123");

        assertThrows(IllegalArgumentException.class, () -> hasher.hashe(null));
        assertThrows(IllegalArgumentException.class, () -> hasher.hashe(""));
        assertFalse(hasher.pruefe(null, hash));
        assertFalse(hasher.pruefe("", hash));
        assertFalse(hasher.pruefe("passwort123", null));
        assertFalse(hasher.pruefe("passwort123", "ungueltiges-format"));
        assertFalse(hasher.pruefe("passwort123", "pbkdf2$nichtzahl$abc$def"));
    }
}
