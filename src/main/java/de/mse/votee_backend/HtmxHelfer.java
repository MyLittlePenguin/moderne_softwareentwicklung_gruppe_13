package de.mse.votee_backend;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

import io.helidon.common.parameters.Parameters;
import io.helidon.http.HeaderName;
import io.helidon.http.HeaderNames;
import io.helidon.http.SetCookie;
import io.helidon.http.Status;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

import de.mse.votee_backend.kontoverwaltung.application.Session;
import de.mse.votee_backend.kontoverwaltung.application.SessionStore;

/**
 * Gemeinsame Hilfsmethoden für htmx 4, Session-Cookies und CSRF-Prüfung.
 */
final class HtmxHelfer {

    static final HeaderName HX_REQUEST = HeaderNames.create("HX-Request");
    static final HeaderName HX_REDIRECT = HeaderNames.create("HX-Redirect");
    static final HeaderName X_CSRF_TOKEN = HeaderNames.create("X-CSRF-Token");
    static final String SESSION_COOKIE = "votee_session";
    static final String CSRF_FELD = "_csrf";

    private HtmxHelfer() {
    }

    static boolean istHtmxAnfrage(ServerRequest request) {
        return "true".equalsIgnoreCase(request.headers().first(HX_REQUEST).orElse(""));
    }

    static Optional<Session> aktuelleSession(ServerRequest request, SessionStore sessionStore) {
        return request.headers().cookies()
                .first(SESSION_COOKIE)
                .asOptional()
                .flatMap(sessionStore::findeGueltigeSession);
    }

    static boolean istGueltigesCsrf(ServerRequest request,
                                    Parameters formParams,
                                    Session session,
                                    SessionStore sessionStore) {
        String headerToken = request.headers().first(X_CSRF_TOKEN).orElse(null);
        if (headerToken != null && !headerToken.isBlank()) {
            return sessionStore.pruefeCsrfToken(session, headerToken);
        }
        String formToken = formParams == null ? null : formParams.first(CSRF_FELD).orElse(null);
        return sessionStore.pruefeCsrfToken(session, formToken);
    }

    static String csrfHeadersJson(Session session) {
        if (session == null) {
            return null;
        }
        return "{\"X-CSRF-Token\":\"" + session.csrfToken() + "\"}";
    }

    static void sendeHtml(ServerResponse response, Status status, String html) {
        response.status(status);
        response.header(HeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        response.header(HeaderNames.VARY, "HX-Request");
        response.send(html);
    }

    static void weiterleiten(ServerRequest request, ServerResponse response, String ziel) {
        response.header(HeaderNames.VARY, "HX-Request");
        if (istHtmxAnfrage(request)) {
            response.status(Status.NO_CONTENT_204);
            response.header(HX_REDIRECT, ziel);
            response.send();
            return;
        }
        response.status(Status.SEE_OTHER_303);
        response.headers().location(URI.create(ziel));
        response.send();
    }

    static void sendeUnangemeldet(ServerRequest request, ServerResponse response) {
        response.status(Status.UNAUTHORIZED_401);
        response.header(HeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        response.header(HeaderNames.VARY, "HX-Request");
        if (istHtmxAnfrage(request)) {
            response.header(HX_REDIRECT, "/login");
        }
        response.send("<p>Anmeldung erforderlich. <a href=\"/login\">Zur Anmeldung</a></p>");
    }

    static void sendeVerboten(ServerResponse response, String meldung) {
        sendeHtml(response, Status.FORBIDDEN_403, "<p class=\"fehler\">" + meldung + "</p>");
    }

    static void setzeSessionCookie(ServerResponse response, Session session, Duration maxAge) {
        SetCookie cookie = SetCookie.builder(SESSION_COOKIE, session.sessionId())
                .httpOnly(true)
                .sameSite(SetCookie.SameSite.LAX)
                .path("/")
                .maxAge(maxAge)
                .build();
        response.headers().addCookie(cookie);
    }

    static void loescheSessionCookie(ServerResponse response) {
        SetCookie cookie = SetCookie.builder(SESSION_COOKIE, "")
                .httpOnly(true)
                .sameSite(SetCookie.SameSite.LAX)
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
        response.headers().addCookie(cookie);
    }
}
