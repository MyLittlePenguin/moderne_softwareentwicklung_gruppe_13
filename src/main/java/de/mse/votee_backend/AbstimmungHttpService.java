package de.mse.votee_backend;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.common.parameters.Parameters;
import io.helidon.http.Status;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

import de.mse.votee_backend.abstimmungsverwaltung.application.AbstimmungEingabeValidator;
import de.mse.votee_backend.abstimmungsverwaltung.application.AbstimmungService;
import de.mse.votee_backend.abstimmungsverwaltung.application.ErstelleAbstimmungBefehl;
import de.mse.votee_backend.abstimmungsverwaltung.application.KeineBerechtigungException;
import de.mse.votee_backend.abstimmungsverwaltung.domain.model.AbstimmungsregelVerletztException;
import de.mse.votee_backend.abstimmungsverwaltung.domain.model.NutzerId;
import de.mse.votee_backend.kontoverwaltung.application.Session;
import de.mse.votee_backend.kontoverwaltung.application.SessionStore;

/**
 * HTTP-Adapter (Thymeleaf + htmx 4) zum Erstellen von Abstimmungen durch Administrator:innen.
 */
class AbstimmungHttpService implements HttpService {

    private static final DateTimeFormatter DATETIME_LOCAL_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final Templates templates;
    private final AbstimmungService abstimmungService;
    private final AbstimmungEingabeValidator validator;
    private final SessionStore sessionStore;

    AbstimmungHttpService(Templates templates,
                          AbstimmungService abstimmungService,
                          AbstimmungEingabeValidator validator,
                          SessionStore sessionStore) {
        this.templates = Objects.requireNonNull(templates, "Templates darf nicht null sein.");
        this.abstimmungService = Objects.requireNonNull(
                abstimmungService, "AbstimmungService darf nicht null sein.");
        this.validator = Objects.requireNonNull(validator, "AbstimmungEingabeValidator darf nicht null sein.");
        this.sessionStore = Objects.requireNonNull(sessionStore, "SessionStore darf nicht null sein.");
    }

    @Override
    public void routing(HttpRules rules) {
        rules.get("/abstimmungen/neu", this::formularAnzeigen)
                .get("/abstimmungen/neu/option-feld", this::neuesOptionFeld)
                .post("/abstimmungen", this::abstimmungErstellen);
    }

    private void formularAnzeigen(ServerRequest request, ServerResponse response) {
        Optional<Session> adminSession = pruefeAdminSession(request, response);
        if (adminSession.isEmpty()) {
            return;
        }

        Map<String, Object> model = baueFormularModel(
                adminSession.get(), "", "", "", "", List.of("", ""), null);
        String html = HtmxHelfer.istHtmxAnfrage(request)
                ? templates.renderFragment("abstimmung-neu", "abstimmung-formular", model)
                : templates.render("abstimmung-neu", model);
        HtmxHelfer.sendeHtml(response, Status.OK_200, html);
    }

    private void neuesOptionFeld(ServerRequest request, ServerResponse response) {
        Optional<Session> adminSession = pruefeAdminSession(request, response);
        if (adminSession.isEmpty()) {
            return;
        }

        String html = templates.renderFragment("abstimmung-neu", "option-zeile", Map.of("wert", ""));
        HtmxHelfer.sendeHtml(response, Status.OK_200, html);
    }

    private void abstimmungErstellen(ServerRequest request, ServerResponse response) {
        Optional<Session> aktiveSession = HtmxHelfer.aktuelleSession(request, sessionStore);
        if (aktiveSession.isEmpty()) {
            HtmxHelfer.sendeUnangemeldet(request, response);
            return;
        }

        Session session = aktiveSession.get();
        Parameters form = leseFormular(request);
        if (!HtmxHelfer.istGueltigesCsrf(request, form, session, sessionStore)) {
            HtmxHelfer.sendeVerboten(response, "Ungültiges CSRF-Token.");
            return;
        }

        if (!session.istAdministrator()) {
            HtmxHelfer.sendeVerboten(response, "Nur Administrator:innen dürfen Abstimmungen erstellen.");
            return;
        }

        String titel = form.first("titel").orElse("");
        String beschreibung = form.first("beschreibung").orElse("");
        String beginnText = form.first("beginn").orElse("");
        String endeText = form.first("ende").orElse("");
        List<String> eingegebeneOptionen = form.all("optionen", List::of);
        List<String> bereinigteOptionen = eingegebeneOptionen.stream()
                .filter(text -> text != null && !text.isBlank())
                .toList();

        try {
            LocalDateTime beginn = parseZeitpunkt(beginnText);
            LocalDateTime ende = parseZeitpunkt(endeText);
            ErstelleAbstimmungBefehl befehl = new ErstelleAbstimmungBefehl(
                    titel, beschreibung, beginn, ende, bereinigteOptionen);

            abstimmungService.erstelleAbstimmung(new NutzerId(session.nutzerId()), befehl);
            HtmxHelfer.weiterleiten(request, response, "/");
        } catch (KeineBerechtigungException e) {
            HtmxHelfer.sendeVerboten(response, e.getMessage());
        } catch (AbstimmungsregelVerletztException e) {
            List<String> optionenFuerAnzeige = eingegebeneOptionen.size() >= 2
                    ? eingegebeneOptionen
                    : List.of("", "");
            Map<String, Object> model = baueFormularModel(
                    session, titel, beschreibung, beginnText, endeText, optionenFuerAnzeige, e.getMessage());
            String html = HtmxHelfer.istHtmxAnfrage(request)
                    ? templates.renderFragment("abstimmung-neu", "abstimmung-formular", model)
                    : templates.render("abstimmung-neu", model);
            HtmxHelfer.sendeHtml(response, Status.UNPROCESSABLE_CONTENT_422, html);
        }
    }

    private Optional<Session> pruefeAdminSession(ServerRequest request, ServerResponse response) {
        Optional<Session> aktiveSession = HtmxHelfer.aktuelleSession(request, sessionStore);
        if (aktiveSession.isEmpty()) {
            HtmxHelfer.sendeUnangemeldet(request, response);
            return Optional.empty();
        }
        if (!aktiveSession.get().istAdministrator()) {
            HtmxHelfer.sendeVerboten(response, "Nur Administrator:innen dürfen Abstimmungen erstellen.");
            return Optional.empty();
        }
        return aktiveSession;
    }

    private LocalDateTime parseZeitpunkt(String text) {
        if (text != null && text.contains("T")) {
            try {
                return LocalDateTime.parse(text.strip(), DATETIME_LOCAL_FORMAT);
            } catch (DateTimeParseException e) {
                throw new AbstimmungsregelVerletztException(
                        "Ungültiges Datumsformat. Erwartet wird dd.MM.yyyy HH:mm.");
            }
        }
        return validator.parseDatum(text);
    }

    private static Parameters leseFormular(ServerRequest request) {
        if (request.headers().testContentType(MediaTypes.APPLICATION_FORM_URLENCODED)) {
            return request.content().as(Parameters.class);
        }
        return Parameters.empty("form");
    }

    private static Map<String, Object> baueFormularModel(Session session,
                                                         String titel,
                                                         String beschreibung,
                                                         String beginn,
                                                         String ende,
                                                         List<String> optionen,
                                                         String fehler) {
        Map<String, Object> model = new HashMap<>();
        model.put("title", "Neue Abstimmung – Votee");
        model.put("session", session);
        model.put("csrfToken", session.csrfToken());
        model.put("csrfHeadersJson", HtmxHelfer.csrfHeadersJson(session));
        model.put("titel", titel);
        model.put("beschreibung", beschreibung);
        model.put("beginn", beginn);
        model.put("ende", ende);
        model.put("optionen", optionen);
        model.put("fehler", fehler);
        return model;
    }
}
