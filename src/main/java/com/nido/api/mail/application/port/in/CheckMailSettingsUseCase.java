package com.nido.api.mail.application.port.in;

import com.nido.api.mail.domain.model.MailSettingsInput;
import com.nido.api.mail.domain.model.MailSettingsProblem;
import com.nido.api.mail.domain.model.Recipient;
import com.nido.api.mail.domain.model.TestMailOutcome;

import java.util.List;
import java.util.Locale;

/** For a context that lets someone type a mail configuration: what is wrong with it, and whether it works. */
public interface CheckMailSettingsUseCase {
    List<MailSettingsProblem> problems(MailSettingsInput input);
    TestMailOutcome sendTest(MailSettingsInput input, Recipient to, Locale locale);
}
