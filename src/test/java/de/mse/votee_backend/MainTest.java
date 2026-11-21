package de.mse.votee_backend;

import io.helidon.config.Config;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.service.registry.Services;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpServer;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ServerTest
class MainTest {

    private final Http1Client client;

    MainTest(Http1Client client) {
        this.client = client;
    }

    @SetUpServer
    static void setUp(WebServerConfig.Builder server) {
        Main.setup(server, Services.get(Config.class));
    }

    @Test
    void indexPageIsRendered() {
        try (Http1ClientResponse response = client.get("/").request()) {
            assertThat(response.status(), is(Status.OK_200));
            assertThat(response.headers().get(HeaderNames.CONTENT_TYPE).get(), containsString("text/html"));
            String html = response.as(String.class);
            assertThat(html, containsString("<h1>Votee</h1>"));
            assertThat(html, containsString("href=\"/css/style.css\""));
        }
    }

    @Test
    void stylesheetIsServed() {
        try (Http1ClientResponse response = client.get("/css/style.css").request()) {
            assertThat(response.status(), is(Status.OK_200));
            assertThat(response.headers().get(HeaderNames.CONTENT_TYPE).get(), containsString("text/css"));
            assertThat(response.as(String.class), containsString("--farbe-primar"));
        }
    }

    @Test
    void unknownRouteReturns404() {
        try (Http1ClientResponse response = client.get("/gibt-es-nicht").request()) {
            assertThat(response.status(), is(Status.NOT_FOUND_404));
        }
    }
}
