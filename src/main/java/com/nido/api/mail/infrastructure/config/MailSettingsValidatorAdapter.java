package com.nido.api.mail.infrastructure.config;

import com.nido.api.mail.domain.model.MailSettingsInput;
import com.nido.api.mail.domain.model.MailSettingsProblem;
import com.nido.api.mail.domain.port.out.MailSettingsValidatorPort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MailSettingsValidatorAdapter implements MailSettingsValidatorPort {
    @Override
    public List<MailSettingsProblem> problems(MailSettingsInput input) {
        return MailSettings.check(input).problems();
    }
}
