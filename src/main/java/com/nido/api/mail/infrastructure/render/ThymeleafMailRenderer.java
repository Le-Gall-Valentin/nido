package com.nido.api.mail.infrastructure.render;

import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.mail.domain.model.RenderedMail;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.dialect.SpringStandardDialect;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

/**
 * Writes a mail from its template under {@code mail/}.
 *
 * <p>A Thymeleaf engine of its own, never shared with anything web: plain {@link TemplateEngine} with
 * the Spring dialect, so expressions are SpEL and read a record's accessors as properties. The record
 * is the template's {@code mail}; the layout also gets {@code appUrl} and {@code logoCid}.
 *
 * <p>Strict on purpose: a missing message, a missing value or a missing {@code <title>} throws. The
 * caller's transaction then fails with it, which is where a broken template should surface — in a
 * test, not in someone's inbox.
 */
public class ThymeleafMailRenderer {

    private static final Set<String> LANGUAGES = Set.of("fr", "en");

    /** One engine for every renderer: a renderer per mail costs nothing, the template cache is shared. */
    private static final TemplateEngine ENGINE = createEngine();
    private final HtmlToTextConverter toText = new HtmlToTextConverter();
    private final String appUrl;

    /** @param appUrl the app's public address, without a trailing slash */
    public ThymeleafMailRenderer(String appUrl) {
        this.appUrl = appUrl;
    }

    private static TemplateEngine createEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("mail/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCacheable(true);

        TemplateEngine engine = new TemplateEngine();
        engine.setDialect(new SpringStandardDialect());
        engine.setTemplateResolver(resolver);
        engine.setMessageResolver(new StrictMessageResolver());
        return engine;
    }

    public RenderedMail render(MailContent content, Locale locale) {
        Context context = new Context(supported(locale));
        context.setVariable("mail", content);
        context.setVariable("appUrl", appUrl);
        context.setVariable("logoCid", MailBranding.LOGO_CID);

        String html = ENGINE.process(content.template(), context);
        Document document = Jsoup.parse(html);
        String subject = document.title().strip();
        if (subject.isEmpty()) {
            throw new IllegalStateException("Mail template '" + content.template()
                + "' renders no <title>, and its <title> is the subject — build it on the layout");
        }
        return new RenderedMail(subject, html, toText.convert(document));
    }

    /** French or English, as the app speaks; anything else is written in English, the app's fallback. */
    static Locale supported(Locale requested) {
        return requested != null && LANGUAGES.contains(requested.getLanguage())
            ? Locale.of(requested.getLanguage())
            : Locale.ENGLISH;
    }
}
