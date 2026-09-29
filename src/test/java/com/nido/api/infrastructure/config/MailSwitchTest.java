package com.nido.api.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class MailSwitchTest {

    @Configuration(proxyBeanMethods = false)
    static class Probe {
        @Bean @ConditionalOnMailEnabled String whenOn() { return "on"; }
        @Bean @ConditionalOnMailDisabled String whenOff() { return "off"; }
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(Probe.class);

    @Test
    void no_host_switches_mail_off() {
        runner.run(context -> assertThat(context).hasBean("whenOff").doesNotHaveBean("whenOn"));
    }

    @Test
    void an_empty_host_switches_mail_off() {
        // What application.yaml produces when NIDO_SMTP_HOST is unset: ${NIDO_SMTP_HOST:} is "".
        runner.withPropertyValues("nido.mail.host=")
            .run(context -> assertThat(context).hasBean("whenOff").doesNotHaveBean("whenOn"));
    }

    @Test
    void a_host_made_of_spaces_switches_mail_off() {
        runner.withPropertyValues("nido.mail.host=\t ")
            .run(context -> assertThat(context).hasBean("whenOff").doesNotHaveBean("whenOn"));
    }

    @Test
    void a_host_switches_mail_on() {
        runner.withPropertyValues("nido.mail.host=smtp.example.com")
            .run(context -> assertThat(context).hasBean("whenOn").doesNotHaveBean("whenOff"));
    }

    @Test
    void the_host_is_read_with_relaxed_binding_like_the_uppercase_keys_of_application_yaml() {
        runner.withPropertyValues("NIDO.mail.host=smtp.example.com")
            .run(context -> assertThat(context).hasBean("whenOn").doesNotHaveBean("whenOff"));
    }

    @Test
    void the_password_never_appears_in_the_properties_string() {
        MailProperties properties = new MailProperties("smtp.example.com", 587, MailProperties.Security.STARTTLS,
            "user", "s3cret", "Nido <nido@example.com>", "https://nido.example.com");

        assertThat(properties.toString()).doesNotContain("s3cret").contains("***");
        assertThat(properties.enabled()).isTrue();
    }
}
