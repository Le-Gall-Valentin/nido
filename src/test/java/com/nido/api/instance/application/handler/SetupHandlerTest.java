package com.nido.api.instance.application.handler;

import com.nido.api.instance.domain.model.CompleteSetupCommand;
import com.nido.api.instance.domain.model.InitialAdmin;
import com.nido.api.instance.domain.model.InstanceException;
import com.nido.api.instance.domain.model.SettingKey;
import com.nido.api.instance.domain.model.SettingProblem;
import com.nido.api.instance.domain.model.SetupCode;
import com.nido.api.shared.model.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SetupHandlerTest {

    private static final String CODE = "K7QM-3XRP-W9TD";
    private static final InitialAdmin JANE = new InitialAdmin("jane", "jane@example.fr", "Str0ng!Password", Language.FR);

    private final InstanceFakes.MemoryState state = new InstanceFakes.MemoryState();
    private final InstanceFakes.MemoryStore store = new InstanceFakes.MemoryStore();
    private final InstanceFakes.FakeMail mail = new InstanceFakes.FakeMail();
    private final InstanceFakes.MemoryCodes codes = new InstanceFakes.MemoryCodes();
    private final InstanceFakes.FakeAdmin admins = new InstanceFakes.FakeAdmin();
    private final Map<SettingKey, String> environment = new HashMap<>();
    private final SetupHandler handler = new SetupHandler(state, codes, () -> environment, store, mail, admins,
        () -> "the-generated-key", Clock.systemUTC());

    @BeforeEach
    void pending() {
        codes.store(new SetupCode(CODE));
    }

    private static CompleteSetupCommand command(String url, Map<SettingKey, String> mailValues, boolean keySaved) {
        return new CompleteSetupCommand(CODE, JANE, url, mailValues, keySaved);
    }

    @Test
    void the_status_says_what_the_environment_already_decided() {
        environment.put(SettingKey.PUBLIC_URL, "https://nido.example.com");
        environment.put(SettingKey.MAIL_HOST, "smtp.example.com");

        var status = handler.status();

        assertThat(status.required()).isTrue();
        assertThat(status.lockedPublicUrl()).contains("https://nido.example.com");
        assertThat(status.mailLocked()).isTrue();
    }

    @Test
    void once_set_up_the_status_says_nothing_else() {
        state.setupCompleted = true;

        assertThat(handler.status().required()).isFalse();
        assertThat(handler.status().lockedPublicUrl()).isEmpty();
    }

    @Test
    void a_wrong_code_is_refused_and_a_finished_setup_answers_as_if_absent() {
        assertThatThrownBy(() -> handler.verifyCode("AAAA-AAAA-AAAA")).isInstanceOf(InstanceException.SetupCodeInvalid.class);
        state.setupCompleted = true;
        assertThatThrownBy(() -> handler.verifyCode(CODE)).isInstanceOf(InstanceException.SetupAlreadyCompleted.class);
    }

    @Test
    void the_key_is_shown_only_when_nido_generated_it() {
        assertThat(handler.encryptionKey(CODE).generated()).isFalse();
        state.keyGenerated = true;
        assertThat(handler.encryptionKey(CODE).key()).isEqualTo("the-generated-key");
    }

    @Test
    void finishing_creates_the_administrator_and_saves_the_address_and_mail_in_one_go() {
        handler.complete(command("http://192.168.1.10:8080/", Map.of(SettingKey.MAIL_HOST, "smtp.example.com",
            SettingKey.MAIL_FROM, "nido@example.com"), false));

        assertThat(state.setupCompleted).isTrue();
        assertThat(admins.created).containsExactly(JANE);
        assertThat(store.rows).containsEntry(SettingKey.PUBLIC_URL, "http://192.168.1.10:8080")
            .containsEntry(SettingKey.MAIL_HOST, "smtp.example.com");
        assertThat(store.lastBy).isNotNull();
    }

    @Test
    void a_generated_key_must_be_confirmed_saved() {
        state.keyGenerated = true;

        assertThatThrownBy(() -> handler.complete(command("https://nido.example.com", null, false)))
            .isInstanceOf(InstanceException.EncryptionKeyNotSaved.class);
        assertThat(admins.created).isEmpty();
    }

    @Test
    void nothing_is_saved_when_something_is_wrong() {
        assertThatThrownBy(() -> handler.complete(command("nido.example.com", null, true)))
            .isInstanceOfSatisfying(InstanceException.SettingsInvalid.class, invalid -> assertThat(invalid.problems())
                .containsExactly(new SettingProblem(SettingKey.PUBLIC_URL, SettingProblem.INVALID_URL)));
        assertThat(state.setupCompleted).isFalse();
        assertThat(admins.created).isEmpty();
        assertThat(store.rows).isEmpty();
    }

    @Test
    void the_second_of_two_browsers_finishing_together_changes_nothing() {
        handler.complete(command("https://nido.example.com", null, true));

        assertThatThrownBy(() -> handler.complete(command("https://other.example.com", null, true)))
            .isInstanceOf(InstanceException.SetupAlreadyCompleted.class);
        assertThat(admins.created).hasSize(1);
        assertThat(store.rows).containsEntry(SettingKey.PUBLIC_URL, "https://nido.example.com");
    }

    @Test
    void mail_set_by_the_environment_cannot_be_typed_in_the_wizard() {
        environment.put(SettingKey.MAIL_HOST, "smtp.example.com");

        assertThatThrownBy(() -> handler.complete(command("https://nido.example.com",
                Map.of(SettingKey.MAIL_HOST, "smtp.other.com"), true)))
            .isInstanceOf(InstanceException.SettingLockedByEnvironment.class);
    }

    @Test
    void an_address_set_by_the_environment_is_kept_whatever_the_wizard_sends() {
        environment.put(SettingKey.PUBLIC_URL, "https://nido.example.com");

        handler.complete(command("http://typed.example.com", null, true));

        assertThat(store.rows).doesNotContainKey(SettingKey.PUBLIC_URL);
    }

    @Test
    void the_test_mail_of_the_wizard_uses_the_address_typed_in_the_wizard() {
        mail.failure = java.util.Optional.of(new com.nido.api.instance.domain.model.MailTestFailure("connection_refused", null));

        assertThatThrownBy(() -> handler.sendTestMail(CODE, Map.of(SettingKey.MAIL_HOST, "smtp.example.com",
                SettingKey.MAIL_FROM, "nido@example.com"), "https://nido.example.com", "jane@example.fr", Locale.FRENCH))
            .isInstanceOf(InstanceException.MailTestFailed.class);
        assertThat(mail.tried.publicUrl()).isEqualTo("https://nido.example.com");
        assertThat(store.rows).isEmpty();
    }

    @Test
    void the_test_mail_asks_for_the_code_too() {
        assertThatThrownBy(() -> handler.sendTestMail("AAAA-AAAA-AAAA", Map.of(), "https://nido.example.com",
                "jane@example.fr", Locale.FRENCH))
            .isInstanceOf(InstanceException.SetupCodeInvalid.class);
        assertThat(mail.tried).isNull();
    }
}
