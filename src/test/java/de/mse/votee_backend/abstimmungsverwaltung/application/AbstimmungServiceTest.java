package de.mse.votee_backend.abstimmungsverwaltung.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.dbclient.DbClient;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.mse.votee_backend.abstimmungsverwaltung.domain.model.AbstimmOption;
import de.mse.votee_backend.abstimmungsverwaltung.domain.model.Abstimmung;
import de.mse.votee_backend.abstimmungsverwaltung.domain.model.AbstimmungId;
import de.mse.votee_backend.abstimmungsverwaltung.domain.model.AbstimmungsregelVerletztException;
import de.mse.votee_backend.abstimmungsverwaltung.domain.model.Abstimmungszeitraum;
import de.mse.votee_backend.abstimmungsverwaltung.domain.model.NutzerId;
import de.mse.votee_backend.abstimmungsverwaltung.domain.repository.AbstimmungRepository;
import de.mse.votee_backend.abstimmungsverwaltung.infrastructure.DatenbankInitialisierung;
import de.mse.votee_backend.abstimmungsverwaltung.infrastructure.H2AbstimmungRepository;

/**
 * Tests für den Anwendungsservice {@link AbstimmungService} mit H2-In-Memory-DB und fixer {@link Clock}.
 */
@DisplayName("AbstimmungService")
class AbstimmungServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
    private static final LocalDateTime JETZT = LocalDateTime.of(2027, 1, 15, 10, 0);
    private static final Clock FIXE_CLOCK = Clock.fixed(JETZT.atZone(ZONE).toInstant(), ZONE);

    private static final NutzerId ADMIN = new NutzerId("admin-1");
    private static final NutzerId BUERGER = new NutzerId("buerger-42");

    private DbClient dbClient;
    private AbstimmungService service;

    @BeforeEach
    void setUp() {
        String dbName = "votee_service_test_" + UUID.randomUUID().toString().replace("-", "");
        Config config = Config.just(ConfigSources.create(Map.of(
                "db.source", "jdbc",
                "db.connection.url", "jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE",
                "db.connection.username", "sa",
                "db.connection.password", ""
        )));
        dbClient = DbClient.create(config.get("db"));
        DatenbankInitialisierung.initialisiere(dbClient);

        AbstimmungRepository repository = new H2AbstimmungRepository(dbClient);
        BerechtigungsPruefung berechtigung = nutzerId -> ADMIN.equals(nutzerId);
        service = new AbstimmungService(repository, berechtigung, FIXE_CLOCK);
    }

    @AfterEach
    void tearDown() {
        if (dbClient != null) {
            dbClient.close();
        }
    }

    private static ErstelleAbstimmungBefehl gueltigerBefehl() {
        return new ErstelleAbstimmungBefehl(
                "Neubau Hallenbad",
                "Soll die Stadt ein neues Hallenbad bauen?",
                JETZT.plusDays(1),
                JETZT.plusDays(14),
                List.of("Ja", "Nein", "Enthaltung"));
    }

    @Test
    @DisplayName("Administrator:in kann eine gültige Abstimmung erstellen und speichern")
    void adminKannAbstimmungErstellenUndSpeichern() {
        AbstimmungId id = service.erstelleAbstimmung(ADMIN, gueltigerBefehl());

        assertNotNull(id);
        Optional<Abstimmung> geladen = service.findeNachId(id);
        assertTrue(geladen.isPresent());
        Abstimmung abstimmung = geladen.get();
        assertEquals(ADMIN, abstimmung.getErstelltVon());
        assertEquals("Neubau Hallenbad", abstimmung.getTitel());
        assertEquals("Soll die Stadt ein neues Hallenbad bauen?", abstimmung.getBeschreibung());
        assertEquals(new Abstimmungszeitraum(JETZT.plusDays(1), JETZT.plusDays(14)), abstimmung.getZeitraum());
        assertEquals(
                List.of(new AbstimmOption("Ja"), new AbstimmOption("Nein"), new AbstimmOption("Enthaltung")),
                abstimmung.getOptionen());
    }

    @Test
    @DisplayName("Grenzfall: Beginn exakt zum aktuellen Zeitpunkt ist erlaubt")
    void beginnExaktJetztIstErlaubt() {
        ErstelleAbstimmungBefehl befehl = new ErstelleAbstimmungBefehl(
                "Sofortstart Abstimmung",
                "",
                JETZT,
                JETZT.plusHours(2),
                List.of("Dafür", "Dagegen"));

        AbstimmungId id = service.erstelleAbstimmung(ADMIN, befehl);

        assertTrue(service.findeNachId(id).isPresent());
    }

    @Test
    @DisplayName("Nicht-Administrator:in darf keine Abstimmung erstellen")
    void nichtAdminDarfKeineAbstimmungErstellen() {
        assertThrows(KeineBerechtigungException.class,
                () -> service.erstelleAbstimmung(BUERGER, gueltigerBefehl()));
        assertTrue(service.findeAlle().isEmpty());
    }

    @Test
    @DisplayName("Fehlende NutzerId (null) wird als fehlende Berechtigung abgelehnt")
    void fehlenderAkteurWirdAbgelehnt() {
        assertThrows(KeineBerechtigungException.class,
                () -> service.erstelleAbstimmung(null, gueltigerBefehl()));
        assertTrue(service.findeAlle().isEmpty());
    }

    @Test
    @DisplayName("Autorisierung wird vor der Eingabevalidierung geprüft")
    void autorisierungWirdVorEingabevalidierungGeprueft() {
        ErstelleAbstimmungBefehl ungueltigerBefehl = new ErstelleAbstimmungBefehl(
                "X",
                "",
                JETZT.minusDays(5),
                JETZT.minusDays(1),
                List.of("Nur eine"));

        assertThrows(KeineBerechtigungException.class,
                () -> service.erstelleAbstimmung(BUERGER, ungueltigerBefehl));
        assertTrue(service.findeAlle().isEmpty());
    }

    @Test
    @DisplayName("Beginn in der Vergangenheit wird abgelehnt und nichts gespeichert")
    void beginnInDerVergangenheitWirdAbgelehnt() {
        ErstelleAbstimmungBefehl befehl = new ErstelleAbstimmungBefehl(
                "Zu spät gestartet",
                "",
                JETZT.minusSeconds(1),
                JETZT.plusDays(2),
                List.of("Ja", "Nein"));

        assertThrows(AbstimmungsregelVerletztException.class,
                () -> service.erstelleAbstimmung(ADMIN, befehl));
        assertTrue(service.findeAlle().isEmpty());
    }

    @Test
    @DisplayName("Ungültiger Titel wird abgelehnt und nichts gespeichert")
    void ungueltigerTitelWirdAbgelehnt() {
        ErstelleAbstimmungBefehl befehl = new ErstelleAbstimmungBefehl(
                "Ab",
                "",
                JETZT.plusDays(1),
                JETZT.plusDays(2),
                List.of("Ja", "Nein"));

        assertThrows(AbstimmungsregelVerletztException.class,
                () -> service.erstelleAbstimmung(ADMIN, befehl));
        assertTrue(service.findeAlle().isEmpty());
    }

    @Test
    @DisplayName("Ungültige Optionen (null, zu wenige, leer, Duplikat) werden abgelehnt")
    void ungueltigeOptionenWerdenAbgelehnt() {
        ErstelleAbstimmungBefehl nullOptionen = new ErstelleAbstimmungBefehl(
                "Gültiger Titel", "", JETZT.plusDays(1), JETZT.plusDays(2), null);
        ErstelleAbstimmungBefehl eineOption = new ErstelleAbstimmungBefehl(
                "Gültiger Titel", "", JETZT.plusDays(1), JETZT.plusDays(2), List.of("Ja"));
        ErstelleAbstimmungBefehl leereOption = new ErstelleAbstimmungBefehl(
                "Gültiger Titel", "", JETZT.plusDays(1), JETZT.plusDays(2), List.of("Ja", "  "));
        ErstelleAbstimmungBefehl doppelteOption = new ErstelleAbstimmungBefehl(
                "Gültiger Titel", "", JETZT.plusDays(1), JETZT.plusDays(2), List.of("Ja", "ja"));

        assertThrows(AbstimmungsregelVerletztException.class, () -> service.erstelleAbstimmung(ADMIN, nullOptionen));
        assertThrows(AbstimmungsregelVerletztException.class, () -> service.erstelleAbstimmung(ADMIN, eineOption));
        assertThrows(AbstimmungsregelVerletztException.class, () -> service.erstelleAbstimmung(ADMIN, leereOption));
        assertThrows(AbstimmungsregelVerletztException.class, () -> service.erstelleAbstimmung(ADMIN, doppelteOption));
        assertTrue(service.findeAlle().isEmpty());
    }

    @Test
    @DisplayName("findeAlle liefert gespeicherte Abstimmungen sortiert nach Enddatum, findeNachId bzw. empty")
    void findeAlleUndFindeNachId() {
        assertTrue(service.findeAlle().isEmpty());
        assertTrue(service.findeNachId(new AbstimmungId("unbekannt")).isEmpty());

        AbstimmungId spaeteresEndeId = service.erstelleAbstimmung(ADMIN, gueltigerBefehl());
        AbstimmungId frueheresEndeId = service.erstelleAbstimmung(ADMIN, new ErstelleAbstimmungBefehl(
                "Radweg Nord",
                null,
                JETZT.plusDays(2),
                JETZT.plusDays(10),
                List.of("Für Ausbau", "Gegen Ausbau")));

        List<Abstimmung> alle = service.findeAlle();
        assertEquals(2, alle.size());
        assertEquals(frueheresEndeId, alle.get(0).getId());
        assertEquals(spaeteresEndeId, alle.get(1).getId());
    }

    @Test
    @DisplayName("Null-Befehl wirft IllegalArgumentException")
    void nullBefehlWirftIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> service.erstelleAbstimmung(ADMIN, null));
    }

    @Test
    @DisplayName("Clock-Verwendung ist deterministisch (Zeitverschiebung ändert Verhalten)")
    void clockSteuertVergangenheitspruefung() {
        Clock spaetereClock = Clock.fixed(Instant.parse("2027-02-01T00:00:00Z"), ZONE);
        AbstimmungService spaetererService = new AbstimmungService(
                new H2AbstimmungRepository(dbClient),
                nutzerId -> ADMIN.equals(nutzerId),
                spaetereClock);

        assertThrows(AbstimmungsregelVerletztException.class,
                () -> spaetererService.erstelleAbstimmung(ADMIN, gueltigerBefehl()));
    }
}
