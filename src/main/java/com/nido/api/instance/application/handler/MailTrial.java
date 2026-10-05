package com.nido.api.instance.application.handler;

import com.nido.api.instance.domain.model.EffectiveSettings;
import com.nido.api.instance.domain.model.InstanceException;
import com.nido.api.instance.domain.model.MailDraft;
import com.nido.api.instance.domain.model.SettingGroup;
import com.nido.api.instance.domain.model.SettingKey;
import com.nido.api.instance.domain.model.SettingProblem;
import com.nido.api.instance.domain.model.SettingsDraft;
import com.nido.api.instance.domain.port.out.MailSettingsCheckPort;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** A mail configuration typed in a form and tried before it is saved: the setup screen's, the settings page's. */
final class MailTrial {

    private MailTrial() {}

    /** Adds the values typed in the mail block to {@code draft}, each checked; a value of another block is refused. */
    static SettingsDraft typed(SettingsDraft draft, Map<SettingKey, String> values) {
        values.forEach((key, value) -> {
            if (key.group() != SettingGroup.MAIL) {
                throw new InstanceException.UnknownSetting(key.code());
            }
            draft.set(key, value);
        });
        return draft;
    }

    /** Sends one mail with {@code tried}. Without a server it is refused; why the mail did not leave is thrown. */
    static void send(MailSettingsCheckPort mail, EffectiveSettings tried, String recipient, Locale locale) {
        if (tried.text(SettingKey.MAIL_HOST).isEmpty()) {
            throw new InstanceException.SettingsInvalid(List.of(new SettingProblem(SettingKey.MAIL_HOST, SettingProblem.REQUIRED)));
        }
        mail.sendTest(MailDraft.of(tried), recipient, locale).ifPresent(failure -> {
            throw new InstanceException.MailTestFailed(failure.reason(), failure.serverReply());
        });
    }
}
