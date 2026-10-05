package com.nido.api.instance.application.handler;

import com.nido.api.instance.application.port.in.SendSettingsTestMailUseCase;
import com.nido.api.instance.domain.model.EffectiveSettings;
import com.nido.api.instance.domain.model.InstanceException;
import com.nido.api.instance.domain.model.SettingKey;
import com.nido.api.instance.domain.model.SettingRules;
import com.nido.api.instance.domain.model.SettingsDraft;
import com.nido.api.instance.domain.model.SettingsResolution;
import com.nido.api.instance.domain.port.out.EnvironmentSettingsPort;
import com.nido.api.instance.domain.port.out.MailSettingsCheckPort;
import com.nido.api.instance.domain.port.out.SettingsStorePort;
import com.nido.api.shared.annotation.ApplicationService;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** The test button of the settings page: the form's values over the saved ones, nothing saved. */
@ApplicationService
public class SendSettingsTestMailHandler implements SendSettingsTestMailUseCase {

    private final EnvironmentSettingsPort environment;
    private final SettingsStorePort store;
    private final MailSettingsCheckPort mail;

    public SendSettingsTestMailHandler(EnvironmentSettingsPort environment, SettingsStorePort store, MailSettingsCheckPort mail) {
        this.environment = environment;
        this.store = store;
        this.mail = mail;
    }

    @Override
    public void sendTest(Map<SettingKey, String> mailValues, String recipient, Locale locale) {
        Map<SettingKey, String> stored = store.load();
        EffectiveSettings current = resolve(stored);
        if (current.locked(SettingKey.MAIL_HOST)) {
            // Set by the environment: the form cannot change it, the test is of what is in place.
            MailTrial.send(mail, current, recipient, locale);
            return;
        }
        SettingsDraft typed = MailTrial.typed(new SettingsDraft(), mailValues);
        if (!typed.problems().isEmpty()) {
            throw new InstanceException.SettingsInvalid(typed.problems());
        }
        EffectiveSettings tried = resolve(typed.applyTo(stored));
        SettingRules.passwordLeftBehind(current, tried, typed).ifPresent(problem -> {
            throw new InstanceException.SettingsInvalid(List.of(problem));
        });
        MailTrial.send(mail, tried, recipient, locale);
    }

    private EffectiveSettings resolve(Map<SettingKey, String> stored) {
        return SettingsResolution.resolve(environment.values(), stored).settings();
    }
}
