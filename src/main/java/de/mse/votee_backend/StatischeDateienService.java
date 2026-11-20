package de.mse.votee_backend;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

/**
 * Liefert statische Ressourcen (wie das Stylesheet) aus dem Classpath aus.
 */
class StatischeDateienService implements HttpService {

    private static final String STYLE_RESSOURCE = "/static/css/style.css";
    private static final String DATUM_PICKER_RESSOURCE = "/static/js/datum-picker.js";

    private final String stylesheet;
    private final String datumPickerSkript;

    StatischeDateienService() {
        this.stylesheet = ladeRessource(STYLE_RESSOURCE);
        this.datumPickerSkript = ladeRessource(DATUM_PICKER_RESSOURCE);
    }

    @Override
    public void routing(HttpRules rules) {
        rules.get("/css/style.css", this::styleCss)
                .get("/js/datum-picker.js", this::datumPickerJs);
    }

    private void styleCss(ServerRequest request, ServerResponse response) {
        sendeStatisch(response, "text/css; charset=UTF-8", stylesheet);
    }

    private void datumPickerJs(ServerRequest request, ServerResponse response) {
        sendeStatisch(response, "text/javascript; charset=UTF-8", datumPickerSkript);
    }

    private static void sendeStatisch(ServerResponse response, String contentType, String inhalt) {
        response.status(Status.OK_200);
        response.header(HeaderNames.CONTENT_TYPE, contentType);
        response.header(HeaderNames.CACHE_CONTROL, "no-cache");
        response.send(inhalt);
    }

    private static String ladeRessource(String pfad) {
        try (InputStream eingabe = StatischeDateienService.class.getResourceAsStream(pfad)) {
            if (eingabe == null) {
                throw new IllegalStateException("Statische Ressource nicht im Classpath gefunden: " + pfad);
            }
            return new String(eingabe.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Fehler beim Lesen der statischen Ressource: " + pfad, e);
        }
    }
}
