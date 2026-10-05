package com.nido.api.mail.infrastructure.config;

import com.nido.api.mail.domain.model.MailSettingsInput;
import com.nido.api.mail.domain.model.MailSettingsProblem;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A mail configuration, checked: a host with no sender or no app URL is not a configuration, and the
 * check says which field is wrong — the settings pages show it next to that field.
 */
public record MailSettings(String host, int port, MailSecurity security, String username, String password,
                           InternetAddress from, URI appUrl) {

    private static final int DEFAULT_PORT = 587;

    public record Check(Optional<MailSettings> settings, List<MailSettingsProblem> problems) {}

    public static Check check(MailSettingsInput input) {
        List<MailSettingsProblem> problems = new ArrayList<>();
        String host = blankToNull(input.host());
        if (host == null) {
            problems.add(new MailSettingsProblem(MailSettingsProblem.Field.HOST, MailSettingsProblem.REQUIRED));
        }
        int port = input.port() == null ? DEFAULT_PORT : input.port();
        if (port < 1 || port > 65_535) {
            problems.add(new MailSettingsProblem(MailSettingsProblem.Field.PORT, MailSettingsProblem.OUT_OF_RANGE));
        }
        Optional<MailSecurity> security = MailSecurity.parse(input.security());
        if (security.isEmpty()) {
            problems.add(new MailSettingsProblem(MailSettingsProblem.Field.SECURITY, MailSettingsProblem.UNKNOWN_SECURITY));
        }
        String username = blankToNull(input.username());
        String password = blankToNull(input.password());
        if ((username == null) != (password == null)) {
            problems.add(new MailSettingsProblem(username == null ? MailSettingsProblem.Field.USERNAME : MailSettingsProblem.Field.PASSWORD, MailSettingsProblem.CREDENTIALS_GO_TOGETHER));
        }
        InternetAddress from = parseFrom(input.from(), problems);
        URI appUrl = parseAppUrl(input.appUrl(), problems);
        if (!problems.isEmpty()) {
            return new Check(Optional.empty(), List.copyOf(problems));
        }
        return new Check(Optional.of(new MailSettings(host.strip(), port, security.get(), username, password, from, appUrl)), List.of());
    }

    private static InternetAddress parseFrom(String value, List<MailSettingsProblem> problems) {
        if (value == null || value.isBlank()) {
            problems.add(new MailSettingsProblem(MailSettingsProblem.Field.FROM, MailSettingsProblem.REQUIRED));
            return null;
        }
        try {
            return new InternetAddress(value.strip(), true);
        } catch (AddressException e) {
            problems.add(new MailSettingsProblem(MailSettingsProblem.Field.FROM, MailSettingsProblem.INVALID_ADDRESS));
            return null;
        }
    }

    /** Plain http is accepted for any host: on a home network the whole app is http anyway. */
    private static URI parseAppUrl(String value, List<MailSettingsProblem> problems) {
        if (value == null || value.isBlank()) {
            problems.add(new MailSettingsProblem(MailSettingsProblem.Field.APP_URL, MailSettingsProblem.REQUIRED));
            return null;
        }
        URI uri;
        try {
            uri = new URI(value.strip());
        } catch (URISyntaxException e) {
            problems.add(new MailSettingsProblem(MailSettingsProblem.Field.APP_URL, MailSettingsProblem.INVALID_URL));
            return null;
        }
        if (!uri.isAbsolute() || uri.getHost() == null
                || !("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))) {
            problems.add(new MailSettingsProblem(MailSettingsProblem.Field.APP_URL, MailSettingsProblem.INVALID_URL));
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
