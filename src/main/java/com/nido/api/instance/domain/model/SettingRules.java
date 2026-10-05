package com.nido.api.instance.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * The rules a setting's value obeys on its own, and the few that tie settings together. What a mail
 * configuration needs beyond that — a sender that parses as an address — is the mail context's to say.
 */
public final class SettingRules {

    /** Also the longest life a token cut-off must outlive: see SessionSettingsPort in authentication. */
    public static final int MAX_ACCESS_TOKEN_MINUTES = 1440;
    public static final int MAX_REFRESH_TOKEN_DAYS = 365;

    private static final Set<String> SECURITIES = Set.of("starttls", "tls", "none");

    private SettingRules() {}

    /** The value as stored. A password keeps its spaces: they may be part of it. */
    public static String normalize(SettingKey key, String value) {
        String stripped = value.strip();
        return switch (key) {
            case MAIL_SECURITY, SWAGGER -> stripped.toLowerCase(Locale.ROOT);
            case PUBLIC_URL -> PublicUrl.parse(stripped).map(PublicUrl::value).orElse(stripped);
            case MAIL_PASSWORD -> value;
            default -> stripped;
        };
    }

    /** What is wrong with a value that is not blank — blank means "back to the default" and is never checked here. */
    public static Optional<String> problem(SettingKey key, String value) {
        String v = value.strip();
        return switch (key) {
            case MAIL_PORT -> integerProblem(v, 1, 65_535);
            case MAIL_SECURITY -> SECURITIES.contains(v.toLowerCase(Locale.ROOT))
                ? Optional.empty() : Optional.of(SettingProblem.UNKNOWN_SECURITY);
            case PUBLIC_URL -> PublicUrl.parse(v).isPresent() ? Optional.empty() : Optional.of(SettingProblem.INVALID_URL);
            case ACCESS_TOKEN_MINUTES -> integerProblem(v, 1, MAX_ACCESS_TOKEN_MINUTES);
            case REFRESH_TOKEN_DAYS -> integerProblem(v, 1, MAX_REFRESH_TOKEN_DAYS);
            case SWAGGER -> "true".equalsIgnoreCase(v) || "false".equalsIgnoreCase(v)
                ? Optional.empty() : Optional.of(SettingProblem.NOT_A_BOOLEAN);
            case MAIL_HOST, MAIL_USERNAME, MAIL_PASSWORD, MAIL_FROM -> Optional.empty();
        };
    }

    /** Rules across settings, on a whole whose values each hold on their own. */
    public static List<SettingProblem> crossProblems(EffectiveSettings settings) {
        List<SettingProblem> problems = new ArrayList<>();
        long accessMinutes = settings.integer(SettingKey.ACCESS_TOKEN_MINUTES);
        long sessionMinutes = settings.integer(SettingKey.REFRESH_TOKEN_DAYS) * 1440L;
        if (accessMinutes >= sessionMinutes) {
            problems.add(new SettingProblem(SettingKey.ACCESS_TOKEN_MINUTES, SettingProblem.ACCESS_NOT_SHORTER_THAN_SESSION));
        }
        if (settings.text(SettingKey.MAIL_HOST).isPresent() && settings.text(SettingKey.PUBLIC_URL).isEmpty()) {
            problems.add(new SettingProblem(SettingKey.PUBLIC_URL, SettingProblem.PUBLIC_URL_REQUIRED_BY_MAIL));
        }
        return problems;
    }

    private static Optional<String> integerProblem(String value, int min, int max) {
        int number;
        try {
            number = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return Optional.of(SettingProblem.NOT_A_NUMBER);
        }
        return number < min || number > max ? Optional.of(SettingProblem.OUT_OF_RANGE) : Optional.empty();
    }
}
