package com.nido.api.mail.infrastructure.transport;

import com.nido.api.mail.infrastructure.config.MailSettings;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;

/**
 * Builds the SMTP client from nido.mail.*, never from spring.mail.*.
 *
 * <p>STARTTLS is <i>required</i>, not merely enabled: enabled alone lets a server that does not
 * offer it — or a network that strips the offer — carry the password and the mail in clear.
 * The certificate must name the host (checked explicitly, whatever the library's default becomes).
 * Every wait is bounded, because JavaMail's default is to wait forever.
 */
public final class SmtpSenderFactory {

    static final Duration TIMEOUT = Duration.ofSeconds(10);

    private SmtpSenderFactory() {}

    public static JavaMailSenderImpl create(MailSettings settings) {
        return create(settings, TIMEOUT);
    }

    static JavaMailSenderImpl create(MailSettings settings, Duration timeout) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(settings.host());
        sender.setPort(settings.port());
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());

        Properties properties = sender.getJavaMailProperties();
        String millis = String.valueOf(timeout.toMillis());
        properties.setProperty("mail.smtp.connectiontimeout", millis);
        properties.setProperty("mail.smtp.timeout", millis);
        properties.setProperty("mail.smtp.writetimeout", millis);

        if (settings.username() != null) {
            sender.setUsername(settings.username());
            sender.setPassword(settings.password());
            properties.setProperty("mail.smtp.auth", "true");
        }
        switch (settings.security()) {
            case STARTTLS -> {
                properties.setProperty("mail.smtp.starttls.enable", "true");
                properties.setProperty("mail.smtp.starttls.required", "true");
                properties.setProperty("mail.smtp.ssl.checkserveridentity", "true");
            }
            case TLS -> {
                properties.setProperty("mail.smtp.ssl.enable", "true");
                properties.setProperty("mail.smtp.ssl.checkserveridentity", "true");
            }
            case NONE -> { }
        }
        return sender;
    }
}
