package com.nido.api.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Outgoing mail, every value optional. {@code host} (NIDO_SMTP_HOST) is the switch: blank means the
 * mail context builds nothing that could send. {@link MailSwitch} alone reads it that way — see
 * {@link ConditionalOnMailEnabled} — so there is one answer to "is mail on?".
 *
 * <p>Checking that a switched-on configuration is complete belongs to the mail context, which owns
 * the parsing of an address and of the app URL (MailSettings); this record only carries the values,
 * so that the switch can be read from any context without reaching into mail's infrastructure.
 *
 * <p>{@code port} is nullable on purpose: a variable set to empty binds {@code null}, which an
 * {@code int} would refuse at startup even with mail off. MailSettings defaults it to 587.
 */
@ConfigurationProperties(prefix = "nido.mail")
public record MailProperties(
    String host,
    Integer port,
    @DefaultValue("starttls") Security security,
    String username,
    String password,
    String from,
    String appUrl
) {

    /** How the connection to the SMTP server is protected. */
    public enum Security {
        /** Port 587: plain connection upgraded to TLS, the upgrade being required. */
        STARTTLS,
        /** Port 465: TLS from the first byte. */
        TLS,
        /** No encryption at all — local development against Mailpit only. */
        NONE
    }

    @Override
    public String toString() {
        return "MailProperties[host=" + host + ", port=" + port + ", security=" + security
            + ", username=" + username + ", password=" + (password == null || password.isEmpty() ? "" : "***")
            + ", from=" + from + ", appUrl=" + appUrl + "]";
    }
}
