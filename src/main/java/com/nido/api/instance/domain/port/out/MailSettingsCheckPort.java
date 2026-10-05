package com.nido.api.instance.domain.port.out;

import com.nido.api.instance.domain.model.MailDraft;
import com.nido.api.instance.domain.model.MailTestFailure;
import com.nido.api.instance.domain.model.SettingProblem;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** The mail context's judgement on a mail configuration — what parses an address lives there. */
public interface MailSettingsCheckPort {

    List<SettingProblem> problems(MailDraft draft);

    /** Empty when the mail left; why not otherwise. A draft that does not hold throws SettingsInvalid. */
    Optional<MailTestFailure> sendTest(MailDraft draft, String recipient, Locale locale);
}
