package com.nido.api.mail.application.handler;

import com.nido.api.mail.domain.model.MailSettingsInput;
import com.nido.api.mail.domain.model.MailSettingsProblem;
import com.nido.api.mail.domain.model.Recipient;
import com.nido.api.mail.domain.model.TestMailOutcome;
import com.nido.api.mail.domain.port.out.MailSettingsValidatorPort;
import com.nido.api.mail.domain.port.out.TestMailPort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class CheckMailSettingsHandlerTest {

    private static final MailSettingsInput INPUT = new MailSettingsInput("smtp.example.com", 587, "starttls", null, null,
        "nido@example.com", "https://nido.example.com");
    private static final Recipient JANE = new Recipient("jane@example.com", null);

    @Test
    void a_configuration_that_does_not_hold_is_not_tried() {
        AtomicBoolean tried = new AtomicBoolean();
        List<MailSettingsProblem> problems = List.of(new MailSettingsProblem("from", MailSettingsProblem.INVALID_ADDRESS));
        CheckMailSettingsHandler handler = new CheckMailSettingsHandler(input -> problems,
            (input, to, locale) -> { tried.set(true); return new TestMailOutcome.Sent(); });

        assertThat(handler.sendTest(INPUT, JANE, Locale.FRENCH)).isEqualTo(new TestMailOutcome.Invalid(problems));
        assertThat(tried).isFalse();
    }

    @Test
    void a_configuration_that_holds_is_tried() {
        CheckMailSettingsHandler handler = new CheckMailSettingsHandler(input -> List.of(),
            (input, to, locale) -> new TestMailOutcome.Failed(TestMailOutcome.AUTHENTICATION_FAILED, "535 Authentication failed"));

        assertThat(handler.sendTest(INPUT, JANE, Locale.FRENCH)).isEqualTo(new TestMailOutcome.Failed(TestMailOutcome.AUTHENTICATION_FAILED, "535 Authentication failed"));
        assertThat(handler.problems(INPUT)).isEmpty();
    }
}
