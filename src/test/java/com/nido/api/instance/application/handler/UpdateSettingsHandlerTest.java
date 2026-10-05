package com.nido.api.instance.application.handler;

import com.nido.api.instance.domain.model.InstanceException;
import com.nido.api.instance.domain.model.MailTestFailure;
import com.nido.api.instance.domain.model.SettingGroup;
import com.nido.api.instance.domain.model.SettingKey;
import com.nido.api.instance.domain.model.SettingProblem;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpdateSettingsHandlerTest {

    private final Map<SettingKey, String> environment = new HashMap<>();
    private final InstanceFakes.MemoryStore store = new InstanceFakes.MemoryStore();
    private final InstanceFakes.FakeMail mail = new InstanceFakes.FakeMail();
    private final UpdateSettingsHandler handler = new UpdateSettingsHandler(() -> environment, store, mail, Clock.systemUTC());
    private final UUID admin = UUID.randomUUID();

    @Test
    void a_block_is_saved_and_the_new_values_apply() {
        var after = handler.update(SettingGroup.SESSIONS, Map.of(SettingKey.ACCESS_TOKEN_MINUTES, "30", SettingKey.REFRESH_TOKEN_DAYS, "7"), admin);

        assertThat(after.integer(SettingKey.ACCESS_TOKEN_MINUTES)).isEqualTo(30);
        assertThat(store.rows).containsEntry(SettingKey.REFRESH_TOKEN_DAYS, "7");
    }

    @Test
    void a_setting_from_the_environment_cannot_be_changed() {
        environment.put(SettingKey.SWAGGER, "true");

        assertThatThrownBy(() -> handler.update(SettingGroup.API, Map.of(SettingKey.SWAGGER, "false"), admin))
            .isInstanceOfSatisfying(InstanceException.SettingLockedByEnvironment.class,
                locked -> assertThat(locked.key()).isEqualTo(SettingKey.SWAGGER));
    }

    @Test
    void every_wrong_value_is_reported_and_nothing_is_saved() {
        assertThatThrownBy(() -> handler.update(SettingGroup.SESSIONS,
                Map.of(SettingKey.ACCESS_TOKEN_MINUTES, "abc", SettingKey.REFRESH_TOKEN_DAYS, "0"), admin))
            .isInstanceOfSatisfying(InstanceException.SettingsInvalid.class, invalid ->
                assertThat(invalid.problems()).containsExactlyInAnyOrder(
                    new SettingProblem(SettingKey.ACCESS_TOKEN_MINUTES, SettingProblem.NOT_A_NUMBER),
                    new SettingProblem(SettingKey.REFRESH_TOKEN_DAYS, SettingProblem.OUT_OF_RANGE)));
        assertThat(store.rows).isEmpty();
    }

    @Test
    void the_access_token_must_stay_shorter_than_the_session() {
        assertThatThrownBy(() -> handler.update(SettingGroup.SESSIONS,
                Map.of(SettingKey.ACCESS_TOKEN_MINUTES, "1440", SettingKey.REFRESH_TOKEN_DAYS, "1"), admin))
            .isInstanceOf(InstanceException.SettingsInvalid.class);
    }

    @Test
    void the_public_address_cannot_go_while_mail_needs_it() {
        store.rows.put(SettingKey.MAIL_HOST, "smtp.example.com");
        store.rows.put(SettingKey.PUBLIC_URL, "https://nido.example.com");

        assertThatThrownBy(() -> handler.reset(SettingKey.PUBLIC_URL, admin))
            .isInstanceOfSatisfying(InstanceException.SettingsInvalid.class, invalid ->
                assertThat(invalid.problems()).contains(new SettingProblem(SettingKey.PUBLIC_URL, SettingProblem.PUBLIC_URL_REQUIRED_BY_MAIL)));
    }

    @Test
    void the_mail_block_is_checked_by_the_mail_context() {
        store.rows.put(SettingKey.PUBLIC_URL, "https://nido.example.com");
        mail.problems = List.of(new SettingProblem(SettingKey.MAIL_FROM, SettingProblem.INVALID_ADDRESS));

        assertThatThrownBy(() -> handler.update(SettingGroup.MAIL,
                Map.of(SettingKey.MAIL_HOST, "smtp.example.com", SettingKey.MAIL_FROM, "nope"), admin))
            .isInstanceOf(InstanceException.SettingsInvalid.class);
    }

    @Test
    void an_empty_password_field_keeps_the_saved_password_and_reset_clears_it() {
        store.rows.put(SettingKey.PUBLIC_URL, "https://nido.example.com");
        store.rows.put(SettingKey.MAIL_HOST, "smtp.example.com");
        store.rows.put(SettingKey.MAIL_PASSWORD, "s3cret");

        handler.update(SettingGroup.MAIL, Map.of(SettingKey.MAIL_HOST, "smtp.example.com", SettingKey.MAIL_USERNAME, "jane",
            SettingKey.MAIL_PASSWORD, "", SettingKey.MAIL_FROM, "nido@example.com"), admin);
        assertThat(store.rows).containsEntry(SettingKey.MAIL_PASSWORD, "s3cret");

        handler.reset(SettingKey.MAIL_PASSWORD, admin);
        assertThat(store.rows).doesNotContainKey(SettingKey.MAIL_PASSWORD);
    }

    @Test
    void a_setting_of_another_block_is_refused() {
        assertThatThrownBy(() -> handler.update(SettingGroup.API, Map.of(SettingKey.MAIL_HOST, "x"), admin))
            .isInstanceOf(InstanceException.UnknownSetting.class);
    }

    @Test
    void the_test_mail_uses_the_form_with_the_saved_password_and_reports_the_server() {
        store.rows.put(SettingKey.PUBLIC_URL, "https://nido.example.com");
        store.rows.put(SettingKey.MAIL_HOST, "smtp.example.com");
        store.rows.put(SettingKey.MAIL_PASSWORD, "s3cret");
        mail.failure = Optional.of(new MailTestFailure("authentication_failed", "535 Authentication failed"));

        assertThatThrownBy(() -> handler.sendTest(Map.of(SettingKey.MAIL_HOST, "smtp.example.com", SettingKey.MAIL_USERNAME, "jane",
                SettingKey.MAIL_PASSWORD, "", SettingKey.MAIL_FROM, "nido@example.com"), "jane@example.com", Locale.FRENCH))
            .isInstanceOfSatisfying(InstanceException.MailTestFailed.class,
                failed -> assertThat(failed.serverReply()).isEqualTo("535 Authentication failed"));
        assertThat(mail.tried.password()).isEqualTo("s3cret");
        assertThat(mail.tried.host()).isEqualTo("smtp.example.com");
        assertThat(store.rows).containsEntry(SettingKey.MAIL_HOST, "smtp.example.com");
    }

    @Test
    void a_test_mail_without_a_host_is_refused() {
        assertThatThrownBy(() -> handler.sendTest(Map.of(), "jane@example.com", Locale.FRENCH))
            .isInstanceOfSatisfying(InstanceException.SettingsInvalid.class, invalid ->
                assertThat(invalid.problems()).containsExactly(new SettingProblem(SettingKey.MAIL_HOST, SettingProblem.REQUIRED)));
    }

    private void aServerIsSaved() {
        store.rows.put(SettingKey.PUBLIC_URL, "https://nido.example.com");
        store.rows.put(SettingKey.MAIL_HOST, "smtp.example.com");
        store.rows.put(SettingKey.MAIL_USERNAME, "jane");
        store.rows.put(SettingKey.MAIL_PASSWORD, "s3cret");
        store.rows.put(SettingKey.MAIL_FROM, "nido@example.com");
    }

    private static final SettingProblem RETYPE =
        new SettingProblem(SettingKey.MAIL_PASSWORD, SettingProblem.PASSWORD_REQUIRED_FOR_NEW_SERVER);

    @Test
    void the_saved_password_is_never_tried_on_another_server() {
        aServerIsSaved();

        assertThatThrownBy(() -> handler.sendTest(Map.of(SettingKey.MAIL_HOST, "smtp.attacker.example",
                SettingKey.MAIL_PASSWORD, ""), "jane@example.com", Locale.FRENCH))
            .isInstanceOfSatisfying(InstanceException.SettingsInvalid.class,
                invalid -> assertThat(invalid.problems()).containsExactly(RETYPE));
        assertThat(mail.tried).isNull();
    }

    @Test
    void the_saved_password_does_not_follow_the_mail_to_another_server_or_a_weaker_protection() {
        aServerIsSaved();

        assertThatThrownBy(() -> handler.update(SettingGroup.MAIL, Map.of(SettingKey.MAIL_HOST, "smtp.attacker.example"), admin))
            .isInstanceOfSatisfying(InstanceException.SettingsInvalid.class,
                invalid -> assertThat(invalid.problems()).contains(RETYPE));
        assertThatThrownBy(() -> handler.update(SettingGroup.MAIL, Map.of(SettingKey.MAIL_SECURITY, "none"), admin))
            .isInstanceOfSatisfying(InstanceException.SettingsInvalid.class,
                invalid -> assertThat(invalid.problems()).contains(RETYPE));
        // Another port of the same host may be another service — on localhost, anyone's.
        assertThatThrownBy(() -> handler.update(SettingGroup.MAIL, Map.of(SettingKey.MAIL_PORT, "2525"), admin))
            .isInstanceOfSatisfying(InstanceException.SettingsInvalid.class,
                invalid -> assertThat(invalid.problems()).contains(RETYPE));
        assertThatThrownBy(() -> handler.sendTest(Map.of(SettingKey.MAIL_HOST, "smtp.example.com", SettingKey.MAIL_PORT, "2525"),
                "jane@example.com", Locale.FRENCH))
            .isInstanceOf(InstanceException.SettingsInvalid.class);

        handler.update(SettingGroup.MAIL, Map.of(SettingKey.MAIL_HOST, "smtp.other.example", SettingKey.MAIL_PASSWORD, "n3w"), admin);
        assertThat(store.rows).containsEntry(SettingKey.MAIL_PASSWORD, "n3w");
    }

    @Test
    void the_saved_password_stays_with_the_server_it_was_saved_for() {
        aServerIsSaved();

        handler.update(SettingGroup.MAIL, Map.of(SettingKey.MAIL_HOST, "SMTP.example.com", SettingKey.MAIL_SECURITY, "STARTTLS",
            SettingKey.MAIL_FROM, "Nido <nido@example.com>"), admin);

        assertThat(store.rows).containsEntry(SettingKey.MAIL_PASSWORD, "s3cret");
    }
}
