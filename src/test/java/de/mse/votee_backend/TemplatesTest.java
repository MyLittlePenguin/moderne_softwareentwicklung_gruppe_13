package de.mse.votee_backend;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

class TemplatesTest {

    @Test
    void rendersVariablesIntoTemplate() {
        String html = new Templates().render("index", Map.of("title", "Testtitel"));

        assertThat(html, containsString("<title>Testtitel</title>"));
    }
}
