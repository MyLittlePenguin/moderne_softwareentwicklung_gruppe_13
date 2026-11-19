package de.mse.votee_backend;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpServer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * HTTP-Integrationstests für Login (mit 5-stelligem 2FA-Code), Rollenprüfung und
 * Abstimmungserstellung über Thymeleaf + htmx 4.
 */
@ServerTest
@DisplayName("HTTP-Adapter: Anmeldung & Abstimmung erstellen (Thymeleaf + htmx 4)")
class AbstimmungErstellenHttpTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
    private static final LocalDateTime JETZT = LocalDateTime.of(2027, 1, 15, 10, 0);
    private static final Clock FIXE_CLOCK = Clock.fixed(JETZT.atZone(ZONE).toInstant(), ZONE);

    private static final Pattern CSRF_PATTERN =
            Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"");

    private final Http1Client client;

    AbstimmungErstellenHttpTest(Http1Client client) {
        this.client = client;
    }

    @SetUpServer
    static void setUp(WebServerConfig.Builder server) {
        String dbName = "votee_http_test_" + UUID.randomUUID().toString().replace("-", "");
        Config config = Config.just(ConfigSources.create(Map.ofEntries(
                Map.entry("db.source", "jdbc"),
                Map.entry("db.connection.url", "jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE"),
                Map.entry("db.connection.username", "sa"),
                Map.entry("db.connection.password", ""),
                Map.entry("votee.auth.session-dauer-minuten", "60"),
                Map.entry("votee.auth.admin.id", "admin-1"),
                Map.entry("votee.auth.admin.benutzername", "admin"),
                Map.entry("votee.auth.admin.passwort", "admin-pass"),
                Map.entry("votee.auth.buerger.id", "buerger-1"),
                Map.entry("votee.auth.buerger.benutzername", "buerger"),
                Map.entry("votee.auth.buerger.passwort", "buerger-pass")
        )));
        Main.setup(server, config, FIXE_CLOCK);
    }

    @Test
    @DisplayName("Login als Admin mit 5-stelligem Code 00000 setzt HttpOnly-Cookie und sendet HX-Redirect")
    void loginMitAdminSetztSessionCookie() {
        String body = formBody(Map.of(
                "benutzername", "admin",
                "passwort", "admin-pass",
                "einmalCode", "00000"));

        try (Http1ClientResponse response = client.post("/login")
                .header(HeaderNames.CONTENT_TYPE, MediaTypes.APPLICATION_FORM_URLENCODED.text())
                .header(HtmxHelfer.HX_REQUEST, "true")
                .submit(body)) {
            assertThat(response.status(), is(Status.NO_CONTENT_204));
            assertThat(response.headers().first(HtmxHelfer.HX_REDIRECT).orElse(""), is("/"));
            String setCookie = response.headers().first(HeaderNames.SET_COOKIE).orElse("");
            assertThat(setCookie, containsString("votee_session="));
            assertThat(setCookie, containsString("HttpOnly"));
            assertThat(setCookie, containsString("SameSite=Lax"));
        }
    }

    @Test
    @DisplayName("Login mit falschem oder nicht 5-stelligem Einmalcode liefert 422-Fragment für htmx 4")
    void loginMitFalschemEinmalCodeLiefert422Fragment() {
        String body = formBody(Map.of(
                "benutzername", "admin",
                "passwort", "admin-pass",
                "einmalCode", "1234"));

        try (Http1ClientResponse response = client.post("/login")
                .header(HeaderNames.CONTENT_TYPE, MediaTypes.APPLICATION_FORM_URLENCODED.text())
                .header(HtmxHelfer.HX_REQUEST, "true")
                .submit(body)) {
            assertThat(response.status(), is(Status.UNPROCESSABLE_CONTENT_422));
            assertThat(response.headers().first(HeaderNames.VARY).orElse(""), containsString("HX-Request"));
            String html = response.as(String.class);
            assertThat(html, containsString("Ungültige Zugangsdaten"));
            assertThat(html, not(containsString("<!DOCTYPE html>")));
        }
    }

    @Test
    @DisplayName("Unangemeldete Anfragen an /abstimmungen/neu und POST /abstimmungen erhalten 401")
    void unangemeldetErhaelt401() {
        try (Http1ClientResponse getResponse = client.get("/abstimmungen/neu")
                .header(HtmxHelfer.HX_REQUEST, "true")
                .request()) {
            assertThat(getResponse.status(), is(Status.UNAUTHORIZED_401));
            assertThat(getResponse.headers().first(HtmxHelfer.HX_REDIRECT).orElse(""), is("/login"));
        }

        String body = formBody(Map.of("titel", "Unberechtigte Abstimmung"));
        try (Http1ClientResponse postResponse = client.post("/abstimmungen")
                .header(HeaderNames.CONTENT_TYPE, MediaTypes.APPLICATION_FORM_URLENCODED.text())
                .header(HtmxHelfer.HX_REQUEST, "true")
                .submit(body)) {
            assertThat(postResponse.status(), is(Status.UNAUTHORIZED_401));
            assertThat(postResponse.headers().first(HtmxHelfer.HX_REDIRECT).orElse(""), is("/login"));
        }
    }

    @Test
    @DisplayName("Bürger:in erhält 403 Forbidden bei Formular und POST /abstimmungen")
    void buergerErhaelt403Forbidden() {
        String cookie = anmeldenUndCookieHolen("buerger", "buerger-pass", "00000");
        String csrf = csrfVonStartseiteHolen(cookie);

        try (Http1ClientResponse getResponse = client.get("/abstimmungen/neu")
                .header(HeaderNames.COOKIE, cookie)
                .request()) {
            assertThat(getResponse.status(), is(Status.FORBIDDEN_403));
        }

        String body = abstimmungFormBody("Bürger-Test", "Beschreibung", "01.03.2027 09:00",
                "10.03.2027 18:00", List.of("Ja", "Nein"));
        try (Http1ClientResponse postResponse = client.post("/abstimmungen")
                .header(HeaderNames.CONTENT_TYPE, MediaTypes.APPLICATION_FORM_URLENCODED.text())
                .header(HeaderNames.COOKIE, cookie)
                .header(HtmxHelfer.X_CSRF_TOKEN, csrf)
                .header(HtmxHelfer.HX_REQUEST, "true")
                .submit(body)) {
            assertThat(postResponse.status(), is(Status.FORBIDDEN_403));
        }
    }

    @Test
    @DisplayName("Admin ohne gültiges CSRF-Token wird mit 403 abgelehnt")
    void adminOhneCsrfWirdAbgelehnt() {
        String cookie = anmeldenUndCookieHolen("admin", "admin-pass", "00000");
        String body = abstimmungFormBody("Ohne CSRF", "", "01.03.2027 09:00",
                "10.03.2027 18:00", List.of("Ja", "Nein"));

        try (Http1ClientResponse response = client.post("/abstimmungen")
                .header(HeaderNames.CONTENT_TYPE, MediaTypes.APPLICATION_FORM_URLENCODED.text())
                .header(HeaderNames.COOKIE, cookie)
                .header(HtmxHelfer.HX_REQUEST, "true")
                .submit(body)) {
            assertThat(response.status(), is(Status.FORBIDDEN_403));
        }
    }

    @Test
    @DisplayName("Admin kann Abstimmung via htmx 4 erstellen und sie erscheint auf der Startseite")
    void adminErstelltAbstimmungViaHtmx4() {
        String cookie = anmeldenUndCookieHolen("admin", "admin-pass", "00000");

        String csrf;
        try (Http1ClientResponse formResponse = client.get("/abstimmungen/neu")
                .header(HeaderNames.COOKIE, cookie)
                .request()) {
            assertThat(formResponse.status(), is(Status.OK_200));
            assertThat(formResponse.headers().first(HeaderNames.VARY).orElse(""), containsString("HX-Request"));
            String html = formResponse.as(String.class);
            assertThat(html, containsString("hx-post=\"/abstimmungen\""));
            assertThat(html, containsString("hx-headers:inherited"));
            csrf = extrahiereCsrf(html);
        }

        try (Http1ClientResponse optionResponse = client.get("/abstimmungen/neu/option-feld")
                .header(HeaderNames.COOKIE, cookie)
                .header(HtmxHelfer.HX_REQUEST, "true")
                .request()) {
            assertThat(optionResponse.status(), is(Status.OK_200));
            assertThat(optionResponse.as(String.class), containsString("name=\"optionen\""));
        }

        String body = abstimmungFormBody("Neubau Bibliothek", "Soll eine neue Stadtbibliothek gebaut werden?",
                "01.03.2027 09:00", "15.03.2027 18:00", List.of("Dafür", "Dagegen", "Enthaltung"));
        try (Http1ClientResponse createResponse = client.post("/abstimmungen")
                .header(HeaderNames.CONTENT_TYPE, MediaTypes.APPLICATION_FORM_URLENCODED.text())
                .header(HeaderNames.COOKIE, cookie)
                .header(HtmxHelfer.X_CSRF_TOKEN, csrf)
                .header(HtmxHelfer.HX_REQUEST, "true")
                .submit(body)) {
            assertThat(createResponse.status(), is(Status.NO_CONTENT_204));
            assertThat(createResponse.headers().first(HtmxHelfer.HX_REDIRECT).orElse(""), is("/"));
        }

        try (Http1ClientResponse indexResponse = client.get("/")
                .header(HeaderNames.COOKIE, cookie)
                .request()) {
            assertThat(indexResponse.status(), is(Status.OK_200));
            String html = indexResponse.as(String.class);
            assertThat(html, containsString("Neubau Bibliothek"));
            assertThat(html, containsString("Dafür"));
            assertThat(html, containsString("Enthaltung"));
        }
    }

    @Test
    @DisplayName("Admin mit Beginn in der Vergangenheit erhält 422-Formularfragment mit erhaltenen Eingaben")
    void adminMitVergangenemBeginnErhaelt422Fragment() {
        String cookie = anmeldenUndCookieHolen("admin", "admin-pass", "00000");
        String csrf = csrfVonStartseiteHolen(cookie);

        String body = abstimmungFormBody("Vergangene Abstimmung", "Darf nicht gespeichert werden",
                "14.01.2027 09:00", "20.01.2027 18:00", List.of("Ja", "Nein"));
        try (Http1ClientResponse response = client.post("/abstimmungen")
                .header(HeaderNames.CONTENT_TYPE, MediaTypes.APPLICATION_FORM_URLENCODED.text())
                .header(HeaderNames.COOKIE, cookie)
                .header(HtmxHelfer.X_CSRF_TOKEN, csrf)
                .header(HtmxHelfer.HX_REQUEST, "true")
                .submit(body)) {
            assertThat(response.status(), is(Status.UNPROCESSABLE_CONTENT_422));
            assertThat(response.headers().first(HeaderNames.VARY).orElse(""), containsString("HX-Request"));
            String html = response.as(String.class);
            assertThat(html, containsString("Vergangenheit"));
            assertThat(html, containsString("Vergangene Abstimmung"));
            assertThat(html, not(containsString("<!DOCTYPE html>")));
        }

        try (Http1ClientResponse indexResponse = client.get("/").request()) {
            assertThat(indexResponse.as(String.class), not(containsString("Vergangene Abstimmung")));
        }
    }

    @Test
    @DisplayName("Logout beendet die Sitzung, sodass geschützte Routen wieder 401 liefern")
    void logoutBeendetSitzung() {
        String cookie = anmeldenUndCookieHolen("admin", "admin-pass", "00000");
        String csrf = csrfVonStartseiteHolen(cookie);

        try (Http1ClientResponse logoutResponse = client.post("/logout")
                .header(HeaderNames.COOKIE, cookie)
                .header(HtmxHelfer.X_CSRF_TOKEN, csrf)
                .header(HtmxHelfer.HX_REQUEST, "true")
                .submit("")) {
            assertThat(logoutResponse.status(), is(Status.NO_CONTENT_204));
            assertThat(logoutResponse.headers().first(HtmxHelfer.HX_REDIRECT).orElse(""), is("/"));
        }

        try (Http1ClientResponse afterLogout = client.get("/abstimmungen/neu")
                .header(HeaderNames.COOKIE, cookie)
                .request()) {
            assertThat(afterLogout.status(), is(Status.UNAUTHORIZED_401));
        }
    }

    private String anmeldenUndCookieHolen(String benutzername, String passwort, String code) {
        String body = formBody(Map.of(
                "benutzername", benutzername,
                "passwort", passwort,
                "einmalCode", code));
        try (Http1ClientResponse response = client.post("/login")
                .header(HeaderNames.CONTENT_TYPE, MediaTypes.APPLICATION_FORM_URLENCODED.text())
                .header(HtmxHelfer.HX_REQUEST, "true")
                .submit(body)) {
            assertThat(response.status(), is(Status.NO_CONTENT_204));
            String setCookie = response.headers().first(HeaderNames.SET_COOKIE).orElseThrow();
            return setCookie.split(";", 2)[0];
        }
    }

    private String csrfVonStartseiteHolen(String cookie) {
        try (Http1ClientResponse response = client.get("/")
                .header(HeaderNames.COOKIE, cookie)
                .request()) {
            assertThat(response.status(), is(Status.OK_200));
            return extrahiereCsrf(response.as(String.class));
        }
    }

    private static String extrahiereCsrf(String html) {
        Matcher matcher = CSRF_PATTERN.matcher(html);
        if (!matcher.find()) {
            throw new AssertionError("Kein CSRF-Token im HTML gefunden:\n" + html);
        }
        return matcher.group(1);
    }

    private static String formBody(Map<String, String> felder) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> eintrag : felder.entrySet()) {
            if (!sb.isEmpty()) {
                sb.append('&');
            }
            sb.append(encode(eintrag.getKey())).append('=').append(encode(eintrag.getValue()));
        }
        return sb.toString();
    }

    private static String abstimmungFormBody(String titel,
                                             String beschreibung,
                                             String beginn,
                                             String ende,
                                             List<String> optionen) {
        StringBuilder sb = new StringBuilder();
        sb.append("titel=").append(encode(titel));
        sb.append("&beschreibung=").append(encode(beschreibung));
        sb.append("&beginn=").append(encode(beginn));
        sb.append("&ende=").append(encode(ende));
        for (String option : optionen) {
            sb.append("&optionen=").append(encode(option));
        }
        return sb.toString();
    }

    private static String encode(String wert) {
        return URLEncoder.encode(wert, StandardCharsets.UTF_8);
    }
}
