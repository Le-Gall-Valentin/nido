package com.nido.api.mail.infrastructure.transport;

import com.nido.api.infrastructure.config.MailProperties;
import com.nido.api.infrastructure.config.MailProperties.Security;
import com.nido.api.mail.infrastructure.config.MailSettings;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.assertj.core.api.Assertions.assertThat;

class SmtpSenderFactoryTest {

    private static MailSettings settings(Security security, String username) {
        return MailSettings.from(new MailProperties("smtp.example.com", 587, security, username, "pw",
            "nido@example.com", "https://nido.example.com"));
    }

    @Test
    void every_wait_is_bounded() {
        // JavaMail waits forever by default: a silent server would pin the dispatch thread for good.
        JavaMailSenderImpl sender = SmtpSenderFactory.create(settings(Security.STARTTLS, null));

        assertThat(sender.getJavaMailProperties())
            .containsEntry("mail.smtp.connectiontimeout", "10000")
            .containsEntry("mail.smtp.timeout", "10000")
            .containsEntry("mail.smtp.writetimeout", "10000");
    }

    @Test
    void starttls_is_required_not_merely_attempted() {
        assertThat(SmtpSenderFactory.create(settings(Security.STARTTLS, null)).getJavaMailProperties())
            .containsEntry("mail.smtp.starttls.enable", "true")
            .containsEntry("mail.smtp.starttls.required", "true")
            .doesNotContainKey("mail.smtp.ssl.enable");
    }

    @Test
    void tls_is_tls_from_the_first_byte() {
        assertThat(SmtpSenderFactory.create(settings(Security.TLS, null)).getJavaMailProperties())
            .containsEntry("mail.smtp.ssl.enable", "true")
            .doesNotContainKey("mail.smtp.starttls.enable");
    }

    @Test
    void none_asks_for_nothing() {
        assertThat(SmtpSenderFactory.create(settings(Security.NONE, null)).getJavaMailProperties())
            .doesNotContainKeys("mail.smtp.ssl.enable", "mail.smtp.starttls.enable", "mail.smtp.auth");
    }

    @Test
    void credentials_turn_authentication_on() {
        JavaMailSenderImpl sender = SmtpSenderFactory.create(settings(Security.STARTTLS, "user"));

        assertThat(sender.getUsername()).isEqualTo("user");
        assertThat(sender.getPassword()).isEqualTo("pw");
        assertThat(sender.getJavaMailProperties()).containsEntry("mail.smtp.auth", "true");
    }
}
