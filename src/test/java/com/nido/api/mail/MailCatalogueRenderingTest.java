package com.nido.api.mail;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.mail.domain.model.MailContent;
import com.nido.api.mail.domain.model.RenderedMail;
import com.nido.api.mail.infrastructure.render.ThymeleafMailRenderer;
import com.nido.api.notifications.domain.model.Notification;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every mail Nido can send, written in every language it speaks, with sample data — including the
 * ones added after this test. A missing message, a missing value or a subject-less template fails
 * here rather than in someone's inbox; and every sentence the HTML shows must also be in the
 * plain-text part, which is what a template writing outside the kit would break.
 */
class MailCatalogueRenderingTest {

    private static final ThymeleafMailRenderer RENDERER = new ThymeleafMailRenderer("https://nido.example");

    static List<Class<?>> productionMails() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(MailContent.class));
        return scanner.findCandidateComponents("com.nido.api").stream()
            .<Class<?>>map(definition -> load(definition.getBeanClassName()))
            .filter(type -> !type.getProtectionDomain().getCodeSource().getLocation().getPath().contains("test-classes"))
            .toList();
    }

    static Stream<Arguments> mailsAndLanguages() {
        return productionMails().stream()
            .flatMap(type -> Stream.of(Locale.FRENCH, Locale.ENGLISH).map(locale -> Arguments.of(type, locale)));
    }

    @ParameterizedTest(name = "{0} in {1}")
    @MethodSource("mailsAndLanguages")
    void every_mail_is_written_whole_in_every_language(Class<?> type, Locale locale) throws Exception {
        RenderedMail mail = RENDERER.render(sample(type), locale);

        assertThat(mail.subject()).isNotBlank();
        assertThat(mail.text()).isNotBlank();
        Document html = Jsoup.parse(mail.html());
        html.select("[data-text-skip]").remove();
        for (Element element : html.body().getAllElements()) {
            String own = element.ownText().strip();
            if (!own.isEmpty()) {
                assertThat(mail.text()).as("text part of %s", type.getSimpleName()).contains(own);
            }
        }
    }

    static Stream<Arguments> notificationMailsAndLanguages() {
        return productionMails().stream()
            .filter(Notification.class::isAssignableFrom)
            .flatMap(type -> Stream.of(Locale.FRENCH, Locale.ENGLISH).map(locale -> Arguments.of(type, locale)));
    }

    /** A notification can be switched off, so its mail says where — in both parts. */
    @ParameterizedTest(name = "{0} in {1}")
    @MethodSource("notificationMailsAndLanguages")
    void a_notification_mail_says_where_to_switch_it_off(Class<?> type, Locale locale) throws Exception {
        RenderedMail mail = RENDERER.render(sample(type), locale);

        assertThat(mail.html()).contains("https://nido.example/account/preferences");
        assertThat(mail.text()).contains("https://nido.example/account/preferences");
    }

    @Test
    void the_catalogue_is_actually_looking_at_mails() {
        assertThat(productionMails()).hasSizeGreaterThanOrEqualTo(2)
            .allSatisfy(type -> assertThat(type.isRecord()).as("%s is a record", type.getSimpleName()).isTrue());
    }

    private static MailContent sample(Class<?> type) throws Exception {
        RecordComponent[] components = type.getRecordComponents();
        Class<?>[] types = Arrays.stream(components).map(RecordComponent::getType).toArray(Class[]::new);
        Object[] values = Arrays.stream(components).map(c -> sampleValue(c.getType(), c.getName())).toArray();
        return (MailContent) type.getDeclaredConstructor(types).newInstance(values);
    }

    private static Object sampleValue(Class<?> type, String name) {
        if (type == String.class) return "sample-" + name;
        if (type == AppPath.class) return new AppPath("/sample/" + name);
        if (type == long.class || type == Long.class) return 30L;
        if (type == int.class || type == Integer.class) return 30;
        if (type.isEnum()) return type.getEnumConstants()[0];
        throw new IllegalStateException("Teach MailCatalogueRenderingTest a sample for " + type.getName() + " (" + name + ")");
    }

    private static Class<?> load(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(name, e);
        }
    }
}
