package com.nido.api.mail.application.handler;

import com.nido.api.mail.application.port.in.CheckMailSettingsUseCase;
import com.nido.api.mail.domain.model.MailSettingsInput;
import com.nido.api.mail.domain.model.MailSettingsProblem;
import com.nido.api.mail.domain.model.Recipient;
import com.nido.api.mail.domain.model.TestMailOutcome;
import com.nido.api.mail.domain.port.out.MailSettingsValidatorPort;
import com.nido.api.mail.domain.port.out.TestMailPort;

import java.util.List;
import java.util.Locale;

public class CheckMailSettingsHandler implements CheckMailSettingsUseCase {

    private final MailSettingsValidatorPort validator;
    private final TestMailPort testMail;

    public CheckMailSettingsHandler(MailSettingsValidatorPort validator, TestMailPort testMail) {
        this.validator = validator;
        this.testMail = testMail;
    }

    @Override
    public List<MailSettingsProblem> problems(MailSettingsInput input) {
        return validator.problems(input);
    }

    @Override
    public TestMailOutcome sendTest(MailSettingsInput input, Recipient to, Locale locale) {
        List<MailSettingsProblem> problems = validator.problems(input);
        return problems.isEmpty() ? testMail.send(input, to, locale) : new TestMailOutcome.Invalid(problems);
    }
}
