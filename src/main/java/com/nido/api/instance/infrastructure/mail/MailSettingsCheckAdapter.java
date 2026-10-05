package com.nido.api.instance.infrastructure.mail;

import com.nido.api.instance.domain.model.InstanceException;
import com.nido.api.instance.domain.model.MailDraft;
import com.nido.api.instance.domain.model.SettingKey;
import com.nido.api.instance.domain.model.SettingProblem;
import com.nido.api.instance.domain.port.out.MailSettingsCheckPort;
import com.nido.api.mail.application.port.in.CheckMailSettingsUseCase;
import com.nido.api.mail.domain.model.MailSettingsInput;
import com.nido.api.mail.domain.model.MailSettingsProblem;
import com.nido.api.mail.domain.model.Recipient;
import com.nido.api.mail.domain.model.TestMailOutcome;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The settings ask the mail context, which owns the parsing of an address, to judge and to try a
 * configuration. Mail reads its settings from here too (MailConfigurationAdapter): the two contexts
 * know each other, each only through the other's published ports — like authentication and identity.
 */
@Component
public class MailSettingsCheckAdapter implements MailSettingsCheckPort {

    private static final Map<String, SettingKey> FIELDS = Map.of(
        "host", SettingKey.MAIL_HOST,
        "port", SettingKey.MAIL_PORT,
        "security", SettingKey.MAIL_SECURITY,
        "username", SettingKey.MAIL_USERNAME,
        "password", SettingKey.MAIL_PASSWORD,
        "from", SettingKey.MAIL_FROM,
        "appUrl", SettingKey.PUBLIC_URL);

    private final CheckMailSettingsUseCase check;

    public MailSettingsCheckAdapter(CheckMailSettingsUseCase check) {
        this.check = check;
    }

    @Override
    public List<SettingProblem> problems(MailDraft draft) {
        return check.problems(input(draft)).stream().map(MailSettingsCheckAdapter::problem).toList();
    }

    @Override
    public Optional<String> sendTest(MailDraft draft, String recipient, Locale locale) {
        return switch (check.sendTest(input(draft), new Recipient(recipient, null), locale)) {
            case TestMailOutcome.Sent sent -> Optional.empty();
            case TestMailOutcome.Invalid invalid ->
                throw new InstanceException.SettingsInvalid(invalid.problems().stream().map(MailSettingsCheckAdapter::problem).toList());
            case TestMailOutcome.Failed failed -> Optional.of(failed.detail());
        };
    }

    private static SettingProblem problem(MailSettingsProblem problem) {
        return new SettingProblem(FIELDS.get(problem.field()), problem.code());
    }

    private static MailSettingsInput input(MailDraft draft) {
        Integer port;
        try {
            port = draft.port() == null ? null : Integer.valueOf(draft.port());
        } catch (NumberFormatException e) {
            throw new InstanceException.SettingsInvalid(List.of(new SettingProblem(SettingKey.MAIL_PORT, SettingProblem.NOT_A_NUMBER)));
        }
        return new MailSettingsInput(draft.host(), port, draft.security(), draft.username(), draft.password(),
            draft.from(), draft.publicUrl());
    }
}
