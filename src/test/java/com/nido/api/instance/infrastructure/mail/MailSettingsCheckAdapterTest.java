package com.nido.api.instance.infrastructure.mail;

import com.nido.api.instance.domain.model.InstanceException;
import com.nido.api.instance.domain.model.MailDraft;
import com.nido.api.instance.domain.model.MailTestFailure;
import com.nido.api.instance.domain.model.SettingKey;
import com.nido.api.instance.domain.model.SettingProblem;
import com.nido.api.mail.application.port.in.CheckMailSettingsUseCase;
import com.nido.api.mail.domain.model.MailSettingsInput;
import com.nido.api.mail.domain.model.MailSettingsProblem;
import com.nido.api.mail.domain.model.Recipient;
import com.nido.api.mail.domain.model.TestMailOutcome;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MailSettingsCheckAdapterTest {

    private static final MailDraft DRAFT =
        new MailDraft("smtp.example.com", "2525", "tls", "jane", "s3cret", "Nido <nido@example.com>", "https://nido.example.com");

    /** The mail context's answers, and what it was asked. */
    private static final class FakeCheck implements CheckMailSettingsUseCase {
        List<MailSettingsProblem> problems = List.of();
        TestMailOutcome outcome = new TestMailOutcome.Sent();
        final List<MailSettingsInput> asked = new ArrayList<>();
        Recipient recipient;

        @Override
        public List<MailSettingsProblem> problems(MailSettingsInput input) {
            asked.add(input);
            return problems;
        }

        @Override
        public TestMailOutcome sendTest(MailSettingsInput input, Recipient to, Locale locale) {
            asked.add(input);
            recipient = to;
            return outcome;
        }
    }

    private final FakeCheck check = new FakeCheck();
    private final MailSettingsCheckAdapter adapter = new MailSettingsCheckAdapter(check);

    @Test
    void the_draft_reaches_the_mail_context_field_for_field() {
        adapter.problems(DRAFT);

        assertThat(check.asked).containsExactly(new MailSettingsInput("smtp.example.com", 2525, "tls", "jane", "s3cret",
            "Nido <nido@example.com>", "https://nido.example.com"));
    }

    @Test
    void each_problem_of_a_mail_field_lands_on_its_setting() {
        check.problems = Arrays.stream(MailSettingsProblem.Field.values())
            .map(field -> new MailSettingsProblem(field, MailSettingsProblem.REQUIRED)).toList();

        assertThat(adapter.problems(DRAFT)).extracting(SettingProblem::key).containsExactly(
            SettingKey.MAIL_HOST, SettingKey.MAIL_PORT, SettingKey.MAIL_SECURITY, SettingKey.MAIL_USERNAME,
            SettingKey.MAIL_PASSWORD, SettingKey.MAIL_FROM, SettingKey.PUBLIC_URL);
    }

    @Test
    void every_code_the_mail_context_gives_is_one_the_pages_word() throws Exception {
        for (Field constant : MailSettingsProblem.class.getFields()) {
            if (constant.getType() == String.class && Modifier.isStatic(constant.getModifiers())) {
                assertThat(SettingProblem.class.getField(constant.getName()).get(null)).isEqualTo(constant.get(null));
            }
        }
    }

    @Test
    void a_port_that_is_not_a_number_never_reaches_the_mail_context() {
        MailDraft draft = new MailDraft("smtp.example.com", "abc", null, null, null, "nido@example.com", "https://nido.example.com");

        assertThatThrownBy(() -> adapter.problems(draft))
            .isInstanceOfSatisfying(InstanceException.SettingsInvalid.class, invalid -> assertThat(invalid.problems())
                .containsExactly(new SettingProblem(SettingKey.MAIL_PORT, SettingProblem.NOT_A_NUMBER)));
        assertThat(check.asked).isEmpty();
    }

    @Test
    void a_test_that_left_is_no_failure_and_goes_to_the_recipient_given() {
        assertThat(adapter.sendTest(DRAFT, "jane@example.fr", Locale.FRENCH)).isEmpty();
        assertThat(check.recipient.address()).isEqualTo("jane@example.fr");
    }

    @Test
    void a_test_refused_before_sending_reports_its_problems_by_setting() {
        check.outcome = new TestMailOutcome.Invalid(List.of(new MailSettingsProblem(MailSettingsProblem.Field.FROM, MailSettingsProblem.REQUIRED)));

        assertThatThrownBy(() -> adapter.sendTest(DRAFT, "jane@example.fr", Locale.FRENCH))
            .isInstanceOfSatisfying(InstanceException.SettingsInvalid.class, invalid -> assertThat(invalid.problems())
                .containsExactly(new SettingProblem(SettingKey.MAIL_FROM, SettingProblem.REQUIRED)));
    }

    @Test
    void a_test_that_failed_keeps_why_and_what_the_server_said() {
        check.outcome = new TestMailOutcome.Failed(TestMailOutcome.AUTHENTICATION_FAILED, "535 Bad credentials");

        assertThat(adapter.sendTest(DRAFT, "jane@example.fr", Locale.FRENCH))
            .contains(new MailTestFailure("authentication_failed", "535 Bad credentials"));
    }
}
