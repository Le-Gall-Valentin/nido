package com.nido.api.mail.infrastructure.config;

import com.nido.api.instance.application.port.in.GetEffectiveSettingsQuery;
import com.nido.api.instance.domain.model.EffectiveSettings;
import com.nido.api.instance.domain.model.SettingKey;
import com.nido.api.mail.domain.model.ActiveMail;
import com.nido.api.mail.domain.model.MailSettingsInput;
import com.nido.api.mail.domain.port.out.MailConfigurationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Mail's settings, read from the instance settings each time they are needed — the environment when
 * it sets them, the settings page otherwise. The last check is kept, keyed by its input, so a mail
 * does not re-parse an address that has not changed.
 *
 * <p>A configuration that does not hold switches mail off, and says so in the log once per distinct
 * configuration: a wrong value in the environment never gets here (the start refuses it), and one
 * saved from the page was checked when it was saved, so this is the rare value a later version
 * stopped accepting.
 */
@Component
public class MailConfigurationAdapter implements MailConfigurationPort, MailSettingsSource {

    private static final Logger log = LoggerFactory.getLogger(MailConfigurationAdapter.class);

    private record Checked(MailSettingsInput input, Optional<MailSettings> settings) {}

    private final GetEffectiveSettingsQuery settings;
    private final AtomicReference<Checked> last = new AtomicReference<>();

    public MailConfigurationAdapter(GetEffectiveSettingsQuery settings) {
        this.settings = settings;
    }

    @Override
    public Optional<MailSettings> settings() {
        Optional<MailSettingsInput> input = input(settings.current());
        if (input.isEmpty()) {
            return Optional.empty();
        }
        Checked cached = last.get();
        if (cached != null && cached.input().equals(input.get())) {
            return cached.settings();
        }
        MailSettings.Check check = MailSettings.check(input.get());
        if (!check.problems().isEmpty()) {
            log.error("Mail is configured but its configuration does not hold, so no mail is sent: {}", check.problems());
        }
        Checked fresh = new Checked(input.get(), check.settings());
        last.set(fresh);
        return fresh.settings();
    }

    @Override
    public Optional<ActiveMail> active() {
        return settings().map(mail -> new ActiveMail(mail.appUrl().toString()));
    }

    static Optional<MailSettingsInput> input(EffectiveSettings settings) {
        Optional<String> host = settings.text(SettingKey.MAIL_HOST);
        if (host.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new MailSettingsInput(
            host.get(),
            settings.text(SettingKey.MAIL_PORT).map(Integer::valueOf).orElse(null),
            settings.text(SettingKey.MAIL_SECURITY).orElse(null),
            settings.text(SettingKey.MAIL_USERNAME).orElse(null),
            settings.text(SettingKey.MAIL_PASSWORD).orElse(null),
            settings.text(SettingKey.MAIL_FROM).orElse(null),
            settings.text(SettingKey.PUBLIC_URL).orElse(null)));
    }
}
