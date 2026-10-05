package com.nido.api.instance.application.handler;

import com.nido.api.instance.application.port.in.SendSettingsTestMailUseCase;
import com.nido.api.instance.application.port.in.UpdateSettingsUseCase;
import com.nido.api.instance.domain.model.EffectiveSettings;
import com.nido.api.instance.domain.model.InstanceException;
import com.nido.api.instance.domain.model.MailDraft;
import com.nido.api.instance.domain.model.SettingGroup;
import com.nido.api.instance.domain.model.SettingKey;
import com.nido.api.instance.domain.model.SettingProblem;
import com.nido.api.instance.domain.model.SettingRules;
import com.nido.api.instance.domain.model.SettingsDraft;
import com.nido.api.instance.domain.model.SettingsResolution;
import com.nido.api.instance.domain.port.out.EnvironmentSettingsPort;
import com.nido.api.instance.domain.port.out.MailSettingsCheckPort;
import com.nido.api.instance.domain.port.out.SettingsStorePort;
import com.nido.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@ApplicationService
public class UpdateSettingsHandler implements UpdateSettingsUseCase, SendSettingsTestMailUseCase {

    private final EnvironmentSettingsPort environment;
    private final SettingsStorePort store;
    private final MailSettingsCheckPort mail;
    private final Clock clock;

    public UpdateSettingsHandler(EnvironmentSettingsPort environment, SettingsStorePort store,
                                 MailSettingsCheckPort mail, Clock clock) {
        this.environment = environment;
        this.store = store;
        this.mail = mail;
        this.clock = clock;
    }

    @Override
    @Transactional
    public EffectiveSettings update(SettingGroup group, Map<SettingKey, String> values, UUID by) {
        Map<SettingKey, String> stored = store.load();
        EffectiveSettings before = resolve(stored);
        SettingsDraft draft = new SettingsDraft();
        values.forEach((key, value) -> {
            if (key.group() != group) {
                throw new InstanceException.UnknownSetting(key.code());
            }
            if (before.locked(key)) {
                throw new InstanceException.SettingLockedByEnvironment(key);
            }
            draft.set(key, value);
        });
        return save(group, draft, stored, by);
    }

    @Override
    @Transactional
    public EffectiveSettings reset(SettingKey key, UUID by) {
        Map<SettingKey, String> stored = store.load();
        if (resolve(stored).locked(key)) {
            throw new InstanceException.SettingLockedByEnvironment(key);
        }
        return save(key.group(), new SettingsDraft().clear(key), stored, by);
    }

    private EffectiveSettings save(SettingGroup group, SettingsDraft draft, Map<SettingKey, String> stored, UUID by) {
        List<SettingProblem> problems = new ArrayList<>(draft.problems());
        EffectiveSettings after = null;
        if (problems.isEmpty()) {
            after = resolve(draft.applyTo(stored));
            problems.addAll(SettingRules.crossProblems(after));
            if (group == SettingGroup.MAIL && after.text(SettingKey.MAIL_HOST).isPresent() && !after.locked(SettingKey.MAIL_HOST)) {
                passwordLeftBehind(resolve(stored), after, draft).ifPresent(problems::add);
                problems.addAll(mail.problems(MailDraft.of(after)));
            }
        }
        if (!problems.isEmpty()) {
            throw new InstanceException.SettingsInvalid(problems.stream().distinct().toList());
        }
        store.save(draft.changes(), by, clock.instant());
        return after;
    }

    @Override
    public void sendTest(Map<SettingKey, String> mailValues, String recipient, Locale locale) {
        Map<SettingKey, String> stored = store.load();
        EffectiveSettings current = resolve(stored);
        MailDraft draft;
        if (current.locked(SettingKey.MAIL_HOST)) {
            draft = MailDraft.of(current);
        } else {
            SettingsDraft typed = new SettingsDraft();
            mailValues.forEach((key, value) -> {
                if (key.group() != SettingGroup.MAIL) {
                    throw new InstanceException.UnknownSetting(key.code());
                }
                typed.set(key, value);
            });
            if (!typed.problems().isEmpty()) {
                throw new InstanceException.SettingsInvalid(typed.problems());
            }
            EffectiveSettings tried = resolve(typed.applyTo(stored));
            if (tried.text(SettingKey.MAIL_HOST).isEmpty()) {
                throw new InstanceException.SettingsInvalid(List.of(new SettingProblem(SettingKey.MAIL_HOST, SettingProblem.REQUIRED)));
            }
            passwordLeftBehind(current, tried, typed).ifPresent(problem -> {
                throw new InstanceException.SettingsInvalid(List.of(problem));
            });
            draft = MailDraft.of(tried);
        }
        mail.sendTest(draft, recipient, locale).ifPresent(failure -> {
            throw new InstanceException.MailTestFailed(failure.reason(), failure.serverReply());
        });
    }

    /**
     * The saved SMTP password goes only to the server — host and port — and with the protection, it was
     * saved for: whoever changes any of them must type it again. Otherwise a stolen administrator session
     * could point mail at a server of its own — another host, or another port of a shared one, localhost
     * above all — and collect the password with the test button, or simply the next mail.
     */
    static Optional<SettingProblem> passwordLeftBehind(EffectiveSettings before, EffectiveSettings after, SettingsDraft draft) {
        if (draft.changes().containsKey(SettingKey.MAIL_PASSWORD) || after.text(SettingKey.MAIL_PASSWORD).isEmpty()
                || after.text(SettingKey.MAIL_HOST).isEmpty() || after.locked(SettingKey.MAIL_HOST)) {
            return Optional.empty();
        }
        boolean sameServer = before.text(SettingKey.MAIL_HOST)
                .map(host -> host.equalsIgnoreCase(after.text(SettingKey.MAIL_HOST).orElseThrow()))
                .orElse(false)
            && Objects.equals(before.text(SettingKey.MAIL_PORT), after.text(SettingKey.MAIL_PORT))
            && Objects.equals(before.text(SettingKey.MAIL_SECURITY), after.text(SettingKey.MAIL_SECURITY));
        return sameServer
            ? Optional.empty()
            : Optional.of(new SettingProblem(SettingKey.MAIL_PASSWORD, SettingProblem.PASSWORD_REQUIRED_FOR_NEW_SERVER));
    }

    private EffectiveSettings resolve(Map<SettingKey, String> stored) {
        return SettingsResolution.resolve(environment.values(), stored).settings();
    }
}
