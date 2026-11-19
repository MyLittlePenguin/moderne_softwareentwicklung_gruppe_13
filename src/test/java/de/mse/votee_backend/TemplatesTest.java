package de.mse.votee_backend;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

class TemplatesTest {

    @Test
    void rendersVariablesIntoTemplate() {
        String html = new Templates().render("index", Map.of("title", "Testtitel"));

        assertThat(html, containsString("<title>Testtitel</title>"));
    }

    @Test
    void rendersFragmentWithoutFullHtmlDocument() {
        String fragment = new Templates().renderFragment("abstimmung-neu", "option-zeile", Map.of("wert", "Ja"));

        assertThat(fragment, containsString("name=\"optionen\""));
        assertThat(fragment, containsString("value=\"Ja\""));
        assertThat(fragment, not(containsString("<!DOCTYPE html>")));
    }
}
