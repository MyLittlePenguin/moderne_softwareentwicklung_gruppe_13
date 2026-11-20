package de.mse.votee_backend;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import io.helidon.config.Config;
import io.helidon.dbclient.DbClient;
import io.helidon.logging.common.LogConfig;
import io.helidon.service.registry.Services;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;

import de.mse.votee_backend.abstimmungsverwaltung.application.AbstimmungEingabeValidator;
import de.mse.votee_backend.abstimmungsverwaltung.application.AbstimmungService;
import de.mse.votee_backend.abstimmungsverwaltung.application.BerechtigungsPruefung;
import de.mse.votee_backend.abstimmungsverwaltung.domain.repository.AbstimmungRepository;
import de.mse.votee_backend.abstimmungsverwaltung.infrastructure.DatenbankInitialisierung;
import de.mse.votee_backend.abstimmungsverwaltung.infrastructure.H2AbstimmungRepository;
import de.mse.votee_backend.kontoverwaltung.application.AuthentifizierungsService;
import de.mse.votee_backend.kontoverwaltung.application.NutzerKontoRepository;
import de.mse.votee_backend.kontoverwaltung.application.PasswortHasher;
import de.mse.votee_backend.kontoverwaltung.application.SessionStore;
import de.mse.votee_backend.kontoverwaltung.application.ZweiFaktorPruefung;
import de.mse.votee_backend.kontoverwaltung.domain.model.NutzerKonto;
import de.mse.votee_backend.kontoverwaltung.domain.model.Rolle;
import de.mse.votee_backend.kontoverwaltung.infrastructure.FesterCodeZweiFaktorPruefung;
import de.mse.votee_backend.kontoverwaltung.infrastructure.KonfigurierteNutzerKonten;
import de.mse.votee_backend.kontoverwaltung.infrastructure.KontoBerechtigungsAdapter;
import de.mse.votee_backend.kontoverwaltung.infrastructure.Pbkdf2PasswortHasher;

public final class Main {

    private static final long STANDARD_SESSION_MINUTEN = 60L;

    private Main() {
    }

    public static void main(String[] args) {
        LogConfig.configureRuntime();

        Config config = Services.get(Config.class);

        WebServerConfig.Builder builder = WebServer.builder()
                .config(config.get("server"));
        setup(builder, config);
        WebServer server = builder.build().start();

        System.out.println("Votee backend runs at http://localhost:" + server.port() + "/");
    }

    static void setup(WebServerConfig.Builder server, Config config) {
        setup(server, config, Clock.systemDefaultZone());
    }

    static void setup(WebServerConfig.Builder server, Config config, Clock clock) {
        DbClient dbClient = DbClient.create(config.get("db"));
        DatenbankInitialisierung.initialisiere(dbClient);

        AbstimmungRepository abstimmungRepository = new H2AbstimmungRepository(dbClient);
        PasswortHasher passwortHasher = new Pbkdf2PasswortHasher();
        NutzerKontoRepository kontoRepository = baueKonten(config.get("votee.auth"), passwortHasher);
        ZweiFaktorPruefung zweiFaktorPruefung = new FesterCodeZweiFaktorPruefung();

        AuthentifizierungsService authentifizierungsService =
                new AuthentifizierungsService(kontoRepository, passwortHasher, zweiFaktorPruefung);
        BerechtigungsPruefung berechtigungsPruefung = new KontoBerechtigungsAdapter(kontoRepository);

        long sessionMinuten = config.get("votee.auth.session-dauer-minuten")
                .asLong()
                .orElse(STANDARD_SESSION_MINUTEN);
        Duration sessionDauer = Duration.ofMinutes(sessionMinuten);
        SessionStore sessionStore = new SessionStore(clock, sessionDauer);

        AbstimmungService abstimmungService =
                new AbstimmungService(abstimmungRepository, berechtigungsPruefung, clock);
        AbstimmungEingabeValidator validator = new AbstimmungEingabeValidator();
        Templates templates = new Templates();

        server.routing(routing -> routing
                .register("/", new StatischeDateienService())
                .register("/", new PageService(templates, abstimmungService, sessionStore))
                .register("/", new AuthHttpService(
                        templates, authentifizierungsService, sessionStore, sessionDauer))
                .register("/", new AbstimmungHttpService(
                        templates, abstimmungService, validator, sessionStore)));
    }

    private static NutzerKontoRepository baueKonten(Config authConfig, PasswortHasher hasher) {
        List<NutzerKonto> konten = new ArrayList<>();

        String adminId = authConfig.get("admin.id").asString().orElse("admin-1");
        String adminName = authConfig.get("admin.benutzername").asString().orElse("admin");
        String adminPasswort = authConfig.get("admin.passwort").asString().orElse("admin-dev-password");
        konten.add(new NutzerKonto(adminId, adminName, hasher.hashe(adminPasswort), Rolle.ADMIN));

        Config buergerConfig = authConfig.get("buerger");
        if (buergerConfig.exists()) {
            String buergerId = buergerConfig.get("id").asString().orElse("buerger-1");
            String buergerName = buergerConfig.get("benutzername").asString().orElse("buerger");
            String buergerPasswort = buergerConfig.get("passwort").asString().orElse("buerger-dev-password");
            konten.add(new NutzerKonto(buergerId, buergerName, hasher.hashe(buergerPasswort), Rolle.BUERGER));
        }

        return new KonfigurierteNutzerKonten(konten);
    }
}
