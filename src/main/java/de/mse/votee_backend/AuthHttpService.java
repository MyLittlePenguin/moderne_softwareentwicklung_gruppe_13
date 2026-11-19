package de.mse.votee_backend;

import java.time.Duration;
import java.util.HashMap;
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

import de.mse.votee_backend.kontoverwaltung.application.AuthentifizierungsService;
import de.mse.votee_backend.kontoverwaltung.application.Session;
import de.mse.votee_backend.kontoverwaltung.application.SessionStore;
import de.mse.votee_backend.kontoverwaltung.domain.model.NutzerKonto;

/**
 * HTTP-Adapter für Anmeldung (Passwort + 5-stelliger 2FA-Einmalcode) und Abmeldung.
 */
class AuthHttpService implements HttpService {

    private final Templates templates;
    private final AuthentifizierungsService authentifizierungsService;
    private final SessionStore sessionStore;
    private final Duration sessionDauer;

    AuthHttpService(Templates templates,
                    AuthentifizierungsService authentifizierungsService,
                    SessionStore sessionStore,
                    Duration sessionDauer) {
        this.templates = Objects.requireNonNull(templates, "Templates darf nicht null sein.");
        this.authentifizierungsService = Objects.requireNonNull(
                authentifizierungsService, "AuthentifizierungsService darf nicht null sein.");
        this.sessionStore = Objects.requireNonNull(sessionStore, "SessionStore darf nicht null sein.");
        this.sessionDauer = Objects.requireNonNull(sessionDauer, "Session-Dauer darf nicht null sein.");
    }

    @Override
    public void routing(HttpRules rules) {
        rules.get("/login", this::loginFormular)
                .post("/login", this::anmelden)
                .post("/logout", this::abmelden);
    }

    private void loginFormular(ServerRequest request, ServerResponse response) {
        Map<String, Object> model = new HashMap<>();
        model.put("title", "Anmeldung – Votee");
        model.put("benutzername", "");
        model.put("fehler", null);

        String html = HtmxHelfer.istHtmxAnfrage(request)
                ? templates.renderFragment("login", "login-formular", model)
                : templates.render("login", model);
        HtmxHelfer.sendeHtml(response, Status.OK_200, html);
    }

    private void anmelden(ServerRequest request, ServerResponse response) {
        Parameters form = leseFormular(request);
        String benutzername = form.first("benutzername").orElse("");
        String passwort = form.first("passwort").orElse("");
        String einmalCode = form.first("einmalCode").orElse("");

        Optional<NutzerKonto> konto = authentifizierungsService.anmelden(benutzername, passwort, einmalCode);
        if (konto.isEmpty()) {
            Map<String, Object> model = new HashMap<>();
            model.put("title", "Anmeldung – Votee");
            model.put("benutzername", benutzername);
            model.put("fehler", "Ungültige Zugangsdaten oder 5-stelliger Einmalcode.");

            String html = HtmxHelfer.istHtmxAnfrage(request)
                    ? templates.renderFragment("login", "login-formular", model)
                    : templates.render("login", model);
            HtmxHelfer.sendeHtml(response, Status.UNPROCESSABLE_CONTENT_422, html);
            return;
        }

        Session session = sessionStore.erzeugeSession(konto.get());
        HtmxHelfer.setzeSessionCookie(response, session, sessionDauer);
        HtmxHelfer.weiterleiten(request, response, "/");
    }

    private void abmelden(ServerRequest request, ServerResponse response) {
        Optional<Session> aktiveSession = HtmxHelfer.aktuelleSession(request, sessionStore);
        if (aktiveSession.isPresent()) {
            Parameters form = leseFormular(request);
            if (!HtmxHelfer.istGueltigesCsrf(request, form, aktiveSession.get(), sessionStore)) {
                HtmxHelfer.sendeVerboten(response, "Ungültiges CSRF-Token.");
                return;
            }
            sessionStore.entferneSession(aktiveSession.get().sessionId());
        }
        HtmxHelfer.loescheSessionCookie(response);
        HtmxHelfer.weiterleiten(request, response, "/");
    }

    private static Parameters leseFormular(ServerRequest request) {
        if (request.headers().testContentType(MediaTypes.APPLICATION_FORM_URLENCODED)) {
            return request.content().as(Parameters.class);
        }
        return Parameters.empty("form");
    }
}
