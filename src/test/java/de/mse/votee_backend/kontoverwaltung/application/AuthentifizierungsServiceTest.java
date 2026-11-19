package de.mse.votee_backend.kontoverwaltung.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.mse.votee_backend.abstimmungsverwaltung.application.BerechtigungsPruefung;
import de.mse.votee_backend.abstimmungsverwaltung.domain.model.NutzerId;
import de.mse.votee_backend.kontoverwaltung.domain.model.NutzerKonto;
import de.mse.votee_backend.kontoverwaltung.domain.model.Rolle;
import de.mse.votee_backend.kontoverwaltung.infrastructure.FesterCodeZweiFaktorPruefung;
import de.mse.votee_backend.kontoverwaltung.infrastructure.KonfigurierteNutzerKonten;
import de.mse.votee_backend.kontoverwaltung.infrastructure.KontoBerechtigungsAdapter;
import de.mse.votee_backend.kontoverwaltung.infrastructure.Pbkdf2PasswortHasher;

/**
 * Tests für die Anmeldung (Passwort + 5-stelliger 2FA-Einmalcode) und den Admin-Berechtigungs-Adapter.
 */
@DisplayName("AuthentifizierungsService & KontoBerechtigungsAdapter")
class AuthentifizierungsServiceTest {

    private AuthentifizierungsService authService;
    private BerechtigungsPruefung berechtigungsPruefung;

    @BeforeEach
    void setUp() {
        PasswortHasher hasher = new Pbkdf2PasswortHasher();
        NutzerKonto admin = new NutzerKonto("admin-1", "admin", hasher.hashe("admin-geheim"), Rolle.ADMIN);
        NutzerKonto buerger = new NutzerKonto("buerger-1", "buerger", hasher.hashe("buerger-geheim"), Rolle.BUERGER);

        NutzerKontoRepository repository = new KonfigurierteNutzerKonten(List.of(admin, buerger));
        ZweiFaktorPruefung zweiFaktor = new FesterCodeZweiFaktorPruefung();

        authService = new AuthentifizierungsService(repository, hasher, zweiFaktor);
        berechtigungsPruefung = new KontoBerechtigungsAdapter(repository);
    }

    @Test
    @DisplayName("Admin-Anmeldung mit korrektem Passwort und 5-stelligem Code 00000 erfolgreich")
    void adminAnmeldungErfolgreich() {
        Optional<NutzerKonto> ergebnis = authService.anmelden("admin", "admin-geheim", "00000");

        assertTrue(ergebnis.isPresent());
        assertEquals("admin-1", ergebnis.get().id());
        assertEquals(Rolle.ADMIN, ergebnis.get().rolle());
        assertTrue(ergebnis.get().istAdministrator());
    }

    @Test
    @DisplayName("Bürger-Anmeldung mit korrektem Passwort und Code 00000 liefert Rolle BUERGER")
    void buergerAnmeldungErfolgreich() {
        Optional<NutzerKonto> ergebnis = authService.anmelden("buerger", "buerger-geheim", "00000");

        assertTrue(ergebnis.isPresent());
        assertEquals(Rolle.BUERGER, ergebnis.get().rolle());
        assertFalse(ergebnis.get().istAdministrator());
    }

    @Test
    @DisplayName("Falsches Passwort oder unbekannter Benutzername wird abgelehnt")
    void falscheZugangsdatenWerdenAbgelehnt() {
        assertTrue(authService.anmelden("admin", "falsch", "00000").isEmpty());
        assertTrue(authService.anmelden("unbekannt", "admin-geheim", "00000").isEmpty());
        assertTrue(authService.anmelden(null, "admin-geheim", "00000").isEmpty());
        assertTrue(authService.anmelden("admin", null, "00000").isEmpty());
    }

    @Test
    @DisplayName("Falscher oder nicht 5-stelliger 2FA-Einmalcode wird abgelehnt")
    void falscherOderUngueltigerEinmalCodeWirdAbgelehnt() {
        assertTrue(authService.anmelden("admin", "admin-geheim", "12345").isEmpty());
        assertTrue(authService.anmelden("admin", "admin-geheim", "0000").isEmpty());
        assertTrue(authService.anmelden("admin", "admin-geheim", "000000").isEmpty());
        assertTrue(authService.anmelden("admin", "admin-geheim", "abcde").isEmpty());
        assertTrue(authService.anmelden("admin", "admin-geheim", "").isEmpty());
        assertTrue(authService.anmelden("admin", "admin-geheim", null).isEmpty());
    }

    @Test
    @DisplayName("KontoBerechtigungsAdapter erkennt nur ADMIN-Konten als Administrator:innen")
    void berechtigungsAdapterPrueftAdminRolle() {
        assertTrue(berechtigungsPruefung.istAdministrator(new NutzerId("admin-1")));
        assertFalse(berechtigungsPruefung.istAdministrator(new NutzerId("buerger-1")));
        assertFalse(berechtigungsPruefung.istAdministrator(new NutzerId("unbekannt")));
        assertFalse(berechtigungsPruefung.istAdministrator(null));
    }
}
