package com.nido.api.instance.application.handler;

import com.nido.api.instance.domain.model.InstanceException;
import com.nido.api.instance.domain.model.MailTestFailure;
import com.nido.api.instance.domain.model.SettingKey;
import com.nido.api.instance.domain.model.SettingProblem;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SendSettingsTestMailHandlerTest {

    private final Map<SettingKey, String> environment = new HashMap<>();
    private final InstanceFakes.MemoryStore store = new InstanceFakes.MemoryStore();
    private final InstanceFakes.FakeMail mail = new InstanceFakes.FakeMail();
    private final SendSettingsTestMailHandler handler = new SendSettingsTestMailHandler(() -> environment, store, mail);

    private static final SettingProblem RETYPE =
        new SettingProblem(SettingKey.MAIL_PASSWORD, SettingProblem.PASSWORD_REQUIRED_FOR_NEW_SERVER);

    private void aServerIsSaved() {
        store.rows.put(SettingKey.PUBLIC_URL, "https://nido.example.com");
        store.rows.put(SettingKey.MAIL_HOST, "smtp.example.com");
        store.rows.put(SettingKey.MAIL_USERNAME, "jane");
        store.rows.put(SettingKey.MAIL_PASSWORD, "s3cret");
        store.rows.put(SettingKey.MAIL_FROM, "nido@example.com");
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
    void the_saved_password_does_not_follow_the_test_to_another_port() {
        aServerIsSaved();

        assertThatThrownBy(() -> handler.sendTest(Map.of(SettingKey.MAIL_HOST, "smtp.example.com", SettingKey.MAIL_PORT, "2525"),
                "jane@example.com", Locale.FRENCH))
            .isInstanceOfSatisfying(InstanceException.SettingsInvalid.class,
                invalid -> assertThat(invalid.problems()).containsExactly(RETYPE));
    }

    @Test
    void mail_set_by_the_environment_is_tested_as_it_is_whatever_the_form_says() {
        environment.put(SettingKey.MAIL_HOST, "smtp.env.example");
        environment.put(SettingKey.MAIL_FROM, "nido@example.com");
        store.rows.put(SettingKey.PUBLIC_URL, "https://nido.example.com");

        handler.sendTest(Map.of(SettingKey.MAIL_HOST, "smtp.attacker.example"), "jane@example.com", Locale.FRENCH);

        assertThat(mail.tried.host()).isEqualTo("smtp.env.example");
    }
}
