package de.mse.votee_backend;

import io.helidon.config.Config;
import io.helidon.logging.common.LogConfig;
import io.helidon.service.registry.Services;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;

public final class Main {

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
        Templates templates = new Templates();

        server.routing(routing -> routing.register("/", new PageService(templates)));
    }
}
