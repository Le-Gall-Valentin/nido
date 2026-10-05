package com.nido.api.instance.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SettingRulesTest {

    @ParameterizedTest
    @CsvSource({
        "MAIL_PORT, 587, ",
        "MAIL_PORT, 0, out_of_range",
        "MAIL_PORT, 70000, out_of_range",
        "MAIL_PORT, abc, not_a_number",
        "MAIL_SECURITY, STARTTLS, ",
        "MAIL_SECURITY, ssl, unknown_security",
        "PUBLIC_URL, nido.example.com, invalid_url",
        "ACCESS_TOKEN_MINUTES, 1440, ",
        "ACCESS_TOKEN_MINUTES, 1441, out_of_range",
        "REFRESH_TOKEN_DAYS, 365, ",
        "REFRESH_TOKEN_DAYS, 0, out_of_range",
        "SWAGGER, TRUE, ",
        "SWAGGER, yes, not_a_boolean",
        "MAIL_FROM, anything, ",
    })
    void each_value_is_checked_on_its_own(SettingKey key, String value, String problem) {
        assertThat(SettingRules.problem(key, value).orElse(null)).isEqualTo(problem);
    }

    @Test
    void values_are_stored_in_one_form() {
        assertThat(SettingRules.normalize(SettingKey.MAIL_SECURITY, " TLS ")).isEqualTo("tls");
        assertThat(SettingRules.normalize(SettingKey.SWAGGER, "True")).isEqualTo("true");
        assertThat(SettingRules.normalize(SettingKey.PUBLIC_URL, "https://Nido.example.com/")).isEqualTo("https://nido.example.com");
        assertThat(SettingRules.normalize(SettingKey.MAIL_HOST, " smtp.example.com ")).isEqualTo("smtp.example.com");
        assertThat(SettingRules.normalize(SettingKey.MAIL_PASSWORD, " pass word ")).isEqualTo(" pass word ");
    }

    private static EffectiveSettings with(Map<SettingKey, String> stored) {
        return SettingsResolution.resolve(Map.of(), stored).settings();
    }

    @Test
    void the_access_token_must_be_shorter_than_the_session() {
        Map<SettingKey, String> stored = new EnumMap<>(SettingKey.class);
        stored.put(SettingKey.ACCESS_TOKEN_MINUTES, "1440");
        stored.put(SettingKey.REFRESH_TOKEN_DAYS, "1");

        assertThat(SettingRules.crossProblems(with(stored)))
            .containsExactly(new SettingProblem(SettingKey.ACCESS_TOKEN_MINUTES, SettingProblem.ACCESS_NOT_SHORTER_THAN_SESSION));
    }

    @Test
    void mail_needs_a_public_address_for_its_links() {
        assertThat(SettingRules.crossProblems(with(Map.of(SettingKey.MAIL_HOST, "smtp.example.com"))))
            .containsExactly(new SettingProblem(SettingKey.PUBLIC_URL, SettingProblem.PUBLIC_URL_REQUIRED_BY_MAIL));
        assertThat(SettingRules.crossProblems(with(Map.of()))).isEmpty();
    }
}
