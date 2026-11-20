package de.mse.votee_backend.abstimmungsverwaltung.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
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
import de.mse.votee_backend.abstimmungsverwaltung.domain.model.Abstimmungszeitraum;
import de.mse.votee_backend.abstimmungsverwaltung.domain.model.NutzerId;
import de.mse.votee_backend.abstimmungsverwaltung.domain.repository.AbstimmungRepository;

/**
 * Integrationstests für das H2-Repository gegen eine isolierte H2-In-Memory-Datenbank.
 */
@DisplayName("H2AbstimmungRepository")
class H2AbstimmungRepositoryTest {

    private DbClient dbClient;
    private AbstimmungRepository repository;

    @BeforeEach
    void setUp() {
        String dbName = "votee_test_" + UUID.randomUUID().toString().replace("-", "");
        Config config = Config.just(ConfigSources.create(Map.of(
                "db.source", "jdbc",
                "db.connection.url", "jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE",
                "db.connection.username", "sa",
                "db.connection.password", ""
        )));
        dbClient = DbClient.create(config.get("db"));
        DatenbankInitialisierung.initialisiere(dbClient);
        repository = new H2AbstimmungRepository(dbClient);
    }

    @AfterEach
    void tearDown() {
        if (dbClient != null) {
            dbClient.close();
        }
    }

    @Test
    @DisplayName("Speichert eine Abstimmung und lädt alle Felder inklusive Optionsreihenfolge")
    void speichereUndFindeNachId() {
        AbstimmungId id = AbstimmungId.neu();
        NutzerId admin = new NutzerId("admin-1");
        Abstimmungszeitraum zeitraum = new Abstimmungszeitraum(
                LocalDateTime.of(2027, 3, 1, 9, 0),
                LocalDateTime.of(2027, 3, 15, 18, 30));
        List<AbstimmOption> optionen = List.of(
                new AbstimmOption("Zuerst Option C"),
                new AbstimmOption("Dann Option A"),
                new AbstimmOption("Zuletzt Option B"));
        Abstimmung original = Abstimmung.erstelle(
                id, admin, "Neubau Hallenbad", "Soll die Stadt ein neues Hallenbad bauen?", zeitraum, optionen);

        repository.speichere(original);

        Optional<Abstimmung> geladen = repository.findeNachId(id);
        assertTrue(geladen.isPresent());
        Abstimmung gefunden = geladen.get();
        assertEquals(id, gefunden.getId());
        assertEquals(admin, gefunden.getErstelltVon());
        assertEquals("Neubau Hallenbad", gefunden.getTitel());
        assertEquals("Soll die Stadt ein neues Hallenbad bauen?", gefunden.getBeschreibung());
        assertEquals(zeitraum, gefunden.getZeitraum());
        assertEquals(optionen, gefunden.getOptionen());
    }

    @Test
    @DisplayName("Erhält leere Beschreibung sowie Umlaute und Sonderzeichen")
    void speichereUndLadeLeereBeschreibungUndUmlaute() {
        AbstimmungId id = new AbstimmungId("abst-umlaute");
        Abstimmungszeitraum zeitraum = new Abstimmungszeitraum(
                LocalDateTime.of(2027, 5, 1, 10, 0),
                LocalDateTime.of(2027, 5, 10, 20, 0));
        List<AbstimmOption> optionen = List.of(
                new AbstimmOption("Für den Ausbau (100%)"),
                new AbstimmOption("Dagegen – zu teuer (€)"));
        Abstimmung abstimmung = Abstimmung.erstelle(
                id, new NutzerId("va-2"), "Bürgerentscheid Café Süd (neu)", null, zeitraum, optionen);

        repository.speichere(abstimmung);

        Abstimmung gefunden = repository.findeNachId(id).orElseThrow();
        assertEquals("", gefunden.getBeschreibung());
        assertEquals("Bürgerentscheid Café Süd (neu)", gefunden.getTitel());
        assertEquals(optionen, gefunden.getOptionen());
    }

    @Test
    @DisplayName("Unbekannte ID liefert Optional.empty()")
    void findeNachIdUnbekanntLiefertEmpty() {
        assertTrue(repository.findeNachId(new AbstimmungId("nicht-vorhanden")).isEmpty());
    }

    @Test
    @DisplayName("findeAlle liefert leere Liste bzw. alle Abstimmungen sortiert nach Enddatum")
    void findeAlleLeerUndMitMehrerenAbstimmungen() {
        assertTrue(repository.findeAlle().isEmpty());

        Abstimmung spaeteresEnde = Abstimmung.erstelle(
                new AbstimmungId("abst-2"),
                new NutzerId("admin-1"),
                "Späteres Ende",
                "Beginnt früher, endet später",
                new Abstimmungszeitraum(
                        LocalDateTime.of(2027, 4, 1, 8, 0),
                        LocalDateTime.of(2027, 6, 20, 18, 0)),
                List.of(new AbstimmOption("Ja"), new AbstimmOption("Nein")));

        Abstimmung frueheresEnde = Abstimmung.erstelle(
                new AbstimmungId("abst-1"),
                new NutzerId("admin-1"),
                "Früheres Ende",
                "Beginnt später, endet früher",
                new Abstimmungszeitraum(
                        LocalDateTime.of(2027, 5, 1, 8, 0),
                        LocalDateTime.of(2027, 5, 10, 18, 0)),
                List.of(new AbstimmOption("Option 1"), new AbstimmOption("Option 2"), new AbstimmOption("Option 3")));

        repository.speichere(spaeteresEnde);
        repository.speichere(frueheresEnde);

        List<Abstimmung> alle = repository.findeAlle();
        assertEquals(2, alle.size());
        assertEquals(frueheresEnde.getId(), alle.get(0).getId());
        assertEquals(3, alle.get(0).getOptionen().size());
        assertEquals(spaeteresEnde.getId(), alle.get(1).getId());
        assertEquals(2, alle.get(1).getOptionen().size());
    }

    @Test
    @DisplayName("Gespeicherte Abstimmung mit Beginn in der Vergangenheit lässt sich laden")
    void ladeAbstimmungMitBeginnInDerVergangenheit() {
        AbstimmungId id = new AbstimmungId("abst-alt");
        Abstimmung vergangen = Abstimmung.erstelle(
                id,
                new NutzerId("admin-1"),
                "Vergangene Abstimmung",
                "Historischer Datensatz",
                new Abstimmungszeitraum(
                        LocalDateTime.of(2020, 1, 1, 8, 0),
                        LocalDateTime.of(2020, 1, 10, 18, 0)),
                List.of(new AbstimmOption("Ja"), new AbstimmOption("Nein")));

        repository.speichere(vergangen);

        Abstimmung geladen = repository.findeNachId(id).orElseThrow();
        assertEquals(vergangen.getZeitraum(), geladen.getZeitraum());
    }

    @Test
    @DisplayName("Schema-Initialisierung ist idempotent und bewahrt vorhandene Daten")
    void initialisierungIstIdempotent() {
        AbstimmungId id = new AbstimmungId("abst-idempotent");
        Abstimmung abstimmung = Abstimmung.erstelle(
                id,
                new NutzerId("admin-1"),
                "Radweg Nord",
                "",
                new Abstimmungszeitraum(
                        LocalDateTime.of(2027, 7, 1, 8, 0),
                        LocalDateTime.of(2027, 7, 15, 18, 0)),
                List.of(new AbstimmOption("Ja"), new AbstimmOption("Nein")));
        repository.speichere(abstimmung);

        DatenbankInitialisierung.initialisiere(dbClient);

        assertTrue(repository.findeNachId(id).isPresent());
    }

    @Test
    @DisplayName("Doppelte AbstimmungId schlägt fehl und lässt bestehenden Datensatz unberührt")
    void doppelteIdWirftFehlerUndRolltTransaktionZurueck() {
        AbstimmungId id = new AbstimmungId("abst-doppelt");
        Abstimmung erste = Abstimmung.erstelle(
                id,
                new NutzerId("admin-1"),
                "Erste Version",
                "Original",
                new Abstimmungszeitraum(
                        LocalDateTime.of(2027, 8, 1, 8, 0),
                        LocalDateTime.of(2027, 8, 10, 18, 0)),
                List.of(new AbstimmOption("Ja"), new AbstimmOption("Nein")));
        Abstimmung zweite = Abstimmung.erstelle(
                id,
                new NutzerId("admin-1"),
                "Zweite Version",
                "Überschrieben",
                new Abstimmungszeitraum(
                        LocalDateTime.of(2027, 9, 1, 8, 0),
                        LocalDateTime.of(2027, 9, 10, 18, 0)),
                List.of(new AbstimmOption("A"), new AbstimmOption("B"), new AbstimmOption("C")));

        repository.speichere(erste);

        assertThrows(RuntimeException.class, () -> repository.speichere(zweite));

        Abstimmung geladen = repository.findeNachId(id).orElseThrow();
        assertEquals("Erste Version", geladen.getTitel());
        assertEquals(List.of(new AbstimmOption("Ja"), new AbstimmOption("Nein")), geladen.getOptionen());
    }
}
