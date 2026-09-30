package com.nido.api.mail.infrastructure.config;

import com.nido.api.infrastructure.config.MailProperties;
import com.nido.api.infrastructure.config.MailProperties.Security;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A switched-on mail configuration, checked. Built once at startup: a host with no sender or no app
 * URL refuses to start rather than failing the first time someone forgets their password.
 */
public record MailSettings(String host, int port, Security security, String username, String password,
                           InternetAddress from, URI appUrl) {

    private static final int DEFAULT_PORT = 587;
    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1");

    public static MailSettings from(MailProperties properties) {
        List<String> problems = new ArrayList<>();
        InternetAddress from = parseFrom(properties.from(), problems);
        URI appUrl = parseAppUrl(properties.appUrl(), problems);
        int port = properties.port() == null ? DEFAULT_PORT : properties.port();
        if (port < 1 || port > 65_535) {
            problems.add("NIDO_SMTP_PORT must be between 1 and 65535");
        }
        if ((blankToNull(properties.username()) == null) != (blankToNull(properties.password()) == null)) {
            problems.add("NIDO_SMTP_USERNAME and NIDO_SMTP_PASSWORD go together");
        }
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Mail is switched on (NIDO_SMTP_HOST is set) but its configuration is incomplete: "
                + String.join("; ", problems));
        }
        return new MailSettings(properties.host().strip(), port,
            properties.security() == null ? Security.STARTTLS : properties.security(),
            blankToNull(properties.username()), blankToNull(properties.password()), from, appUrl);
    }

    private static InternetAddress parseFrom(String value, List<String> problems) {
        if (value == null || value.isBlank()) {
            problems.add("NIDO_MAIL_FROM is required");
            return null;
        }
        try {
            return new InternetAddress(value.strip(), true);
        } catch (AddressException e) {
            problems.add("NIDO_MAIL_FROM is not a valid address: " + value);
            return null;
        }
    }

    private static URI parseAppUrl(String value, List<String> problems) {
        if (value == null || value.isBlank()) {
            problems.add("NIDO_APP_URL is required");
            return null;
        }
        URI uri;
        try {
            uri = new URI(value.strip());
        } catch (URISyntaxException e) {
            problems.add("NIDO_APP_URL must be an absolute http(s) URL: " + value);
            return null;
        }
        if (!uri.isAbsolute() || uri.getHost() == null
                || !("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))) {
            problems.add("NIDO_APP_URL must be an absolute http(s) URL: " + value);
            return null;
        }
        if ("http".equals(uri.getScheme()) && !LOCAL_HOSTS.contains(uri.getHost())) {
            problems.add("NIDO_APP_URL must use https outside localhost: " + value);
            return null;
        }
        String text = uri.toString();
        return URI.create(text.endsWith("/") ? text.substring(0, text.length() - 1) : text);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    @Override
    public String toString() {
        return "MailSettings[host=" + host + ", port=" + port + ", security=" + security
            + ", username=" + username + ", password=" + (password == null ? "" : "***")
            + ", from=" + from + ", appUrl=" + appUrl + "]";
    }
}
