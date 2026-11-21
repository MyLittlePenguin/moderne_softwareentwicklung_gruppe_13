package de.mse.votee_backend;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Rendert Thymeleaf-Templates und -Fragmente (für htmx 4) aus {@code src/main/resources/templates/}.
 */
public class Templates {

    private final TemplateEngine engine = new TemplateEngine();

    public Templates() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        engine.setTemplateResolver(resolver);
    }

    public String render(String template, Map<String, Object> variables) {
        return engine.process(template, new Context(Locale.GERMAN, variables));
    }

    public String renderFragment(String template, String fragment, Map<String, Object> variables) {
        return engine.process(template, Set.of(fragment), new Context(Locale.GERMAN, variables));
    }
}
