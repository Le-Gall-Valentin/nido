package com.nido.api.mail.infrastructure.render;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rules every mail template obeys, including the ones written after this test.
 *
 * <p>{@code th:utext} writes a value as markup. A mail carries text other people typed — a username
 * today, a task title tomorrow — so one {@code th:utext} is one injection into someone's inbox.
 */
class MailTemplateConventionsTest {

    private static List<Resource> templates() throws Exception {
        return List.of(new PathMatchingResourcePatternResolver().getResources("classpath*:mail/**/*.html"));
    }

    @Test
    void no_mail_template_writes_unescaped_text() throws Exception {
        List<String> offenders = new ArrayList<>();
        for (Resource template : templates()) {
            if (template.getContentAsString(StandardCharsets.UTF_8).contains("th:utext")) {
                offenders.add(template.getDescription());
            }
        }

        assertThat(offenders).as("mail templates using th:utext").isEmpty();
    }

    @Test
    void the_rule_actually_looked_at_something() throws Exception {
        assertThat(templates()).hasSizeGreaterThanOrEqualTo(2);
    }
}
