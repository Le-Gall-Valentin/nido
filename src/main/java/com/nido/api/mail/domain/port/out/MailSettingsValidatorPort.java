package com.nido.api.mail.domain.port.out;

import com.nido.api.mail.domain.model.MailSettingsInput;
import com.nido.api.mail.domain.model.MailSettingsProblem;

import java.util.List;

public interface MailSettingsValidatorPort {
    List<MailSettingsProblem> problems(MailSettingsInput input);
}
