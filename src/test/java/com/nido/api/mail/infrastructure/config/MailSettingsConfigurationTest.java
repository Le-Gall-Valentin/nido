package com.nido.api.mail.infrastructure.config;

import com.nido.api.infrastructure.config.MailProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.assertj.core.api.Assertions.assertThat;

class MailSettingsConfigurationTest {

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(MailProperties.class)
    static class Properties {}

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(Properties.class, MailSettingsConfiguration.class);

    @Test
    void without_a_host_nothing_is_built() {
        runner.run(context -> assertThat(context)
            .hasNotFailed()
            .doesNotHaveBean(MailSettings.class)
            .doesNotHaveBean(JavaMailSender.class));
    }

    @Test
    void a_host_without_the_rest_stops_the_start_and_says_what_is_missing() {
        runner.withPropertyValues("nido.mail.host=smtp.example.com").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause()
                .hasMessageContaining("NIDO_MAIL_FROM is required")
                .hasMessageContaining("NIDO_APP_URL is required");
        });
    }

    @Test
    void a_complete_configuration_builds_the_sender() {
        runner.withPropertyValues(
                "nido.mail.host=smtp.example.com",
                "nido.mail.port=2525",
                "nido.mail.from=Nido <nido@example.com>",
                "nido.mail.app-url=https://nido.example.com")
            .run(context -> {
                assertThat(context).hasSingleBean(MailSettings.class).hasSingleBean(JavaMailSenderImpl.class);
                JavaMailSenderImpl sender = context.getBean(JavaMailSenderImpl.class);
                assertThat(sender.getHost()).isEqualTo("smtp.example.com");
                assertThat(sender.getPort()).isEqualTo(2525);
            });
    }
}
