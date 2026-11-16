package de.mse.votee_backend;

import java.util.Locale;
import java.util.Map;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Rendert Thymeleaf-Templates aus {@code src/main/resources/templates/}.
 */
class Templates {

    private final TemplateEngine engine = new TemplateEngine();

    Templates() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        engine.setTemplateResolver(resolver);
    }

    String render(String template, Map<String, Object> variables) {
        return engine.process(template, new Context(Locale.GERMAN, variables));
    }
}
