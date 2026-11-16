package de.mse.votee_backend;

import java.util.Map;

import io.helidon.http.HeaderNames;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

/**
 * Liefert die HTML-Seiten der Anwendung aus.
 */
class PageService implements HttpService {

    private final Templates templates;

    PageService(Templates templates) {
        this.templates = templates;
    }

    @Override
    public void routing(HttpRules rules) {
        rules.get("/", this::index);
    }

    private void index(ServerRequest request, ServerResponse response) {
        String html = templates.render("index", Map.of("title", "Votee"));
        response.header(HeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        response.send(html);
    }
}
