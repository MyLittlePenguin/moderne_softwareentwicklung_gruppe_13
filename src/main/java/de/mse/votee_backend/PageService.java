package de.mse.votee_backend;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.helidon.http.Status;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

import de.mse.votee_backend.abstimmungsverwaltung.application.AbstimmungService;
import de.mse.votee_backend.abstimmungsverwaltung.domain.model.AbstimmOption;
import de.mse.votee_backend.abstimmungsverwaltung.domain.model.Abstimmung;
import de.mse.votee_backend.kontoverwaltung.application.Session;
import de.mse.votee_backend.kontoverwaltung.application.SessionStore;

/**
 * Liefert die Startseite mit der Übersicht aller Abstimmungen aus.
 */
class PageService implements HttpService {

    private static final DateTimeFormatter DATUM_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final Templates templates;
    private final AbstimmungService abstimmungService;
    private final SessionStore sessionStore;

    PageService(Templates templates, AbstimmungService abstimmungService, SessionStore sessionStore) {
        this.templates = Objects.requireNonNull(templates, "Templates darf nicht null sein.");
        this.abstimmungService = Objects.requireNonNull(
                abstimmungService, "AbstimmungService darf nicht null sein.");
        this.sessionStore = Objects.requireNonNull(sessionStore, "SessionStore darf nicht null sein.");
    }

    @Override
    public void routing(HttpRules rules) {
        rules.get("/", this::index);
    }

    private void index(ServerRequest request, ServerResponse response) {
        Optional<Session> session = HtmxHelfer.aktuelleSession(request, sessionStore);
        List<AbstimmungAnsicht> abstimmungen = abstimmungService.findeAlle().stream()
                .map(PageService::zuAnsicht)
                .toList();

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Votee");
        model.put("abstimmungen", abstimmungen);
        model.put("session", session.orElse(null));
        model.put("csrfToken", session.map(Session::csrfToken).orElse(""));
        model.put("csrfHeadersJson", HtmxHelfer.csrfHeadersJson(session.orElse(null)));

        String html = templates.render("index", model);
        HtmxHelfer.sendeHtml(response, Status.OK_200, html);
    }

    private static AbstimmungAnsicht zuAnsicht(Abstimmung abstimmung) {
        return new AbstimmungAnsicht(
                abstimmung.getId().wert(),
                abstimmung.getTitel(),
                abstimmung.getBeschreibung(),
                DATUM_FORMAT.format(abstimmung.getZeitraum().beginn()),
                DATUM_FORMAT.format(abstimmung.getZeitraum().ende()),
                abstimmung.getOptionen().stream().map(AbstimmOption::text).toList());
    }

    /**
     * View-Model für die Darstellung einer Abstimmung in Thymeleaf.
     */
    record AbstimmungAnsicht(
            String id,
            String titel,
            String beschreibung,
            String beginn,
            String ende,
            List<String> optionen) {
    }
}
