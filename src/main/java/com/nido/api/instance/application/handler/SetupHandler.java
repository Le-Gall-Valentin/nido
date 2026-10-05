package com.nido.api.instance.application.handler;

import com.nido.api.instance.application.port.in.GetSetupStatusQuery;
import com.nido.api.instance.application.port.in.SetupUseCase;
import com.nido.api.instance.domain.model.CompleteSetupCommand;
import com.nido.api.instance.domain.model.EffectiveSettings;
import com.nido.api.instance.domain.model.InstanceException;
import com.nido.api.instance.domain.model.InstanceState;
import com.nido.api.instance.domain.model.MailDraft;
import com.nido.api.instance.domain.model.SettingGroup;
import com.nido.api.instance.domain.model.SettingKey;
import com.nido.api.instance.domain.model.SettingProblem;
import com.nido.api.instance.domain.model.SettingRules;
import com.nido.api.instance.domain.model.SettingsDraft;
import com.nido.api.instance.domain.model.SettingsResolution;
import com.nido.api.instance.domain.model.SetupEncryptionKey;
import com.nido.api.instance.domain.model.SetupStatus;
import com.nido.api.instance.domain.port.out.ActiveEncryptionKeyPort;
import com.nido.api.instance.domain.port.out.EnvironmentSettingsPort;
import com.nido.api.instance.domain.port.out.InitialAdminPort;
import com.nido.api.instance.domain.port.out.InstanceStatePort;
import com.nido.api.instance.domain.port.out.MailSettingsCheckPort;
import com.nido.api.instance.domain.port.out.SettingsStorePort;
import com.nido.api.instance.domain.port.out.SetupCodePort;
import com.nido.api.shared.annotation.ApplicationService;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationService
public class SetupHandler implements GetSetupStatusQuery, SetupUseCase {

    private final InstanceStatePort instanceState;
    private final SetupCodePort setupCodes;
    private final EnvironmentSettingsPort environment;
    private final SettingsStorePort store;
    private final MailSettingsCheckPort mail;
    private final InitialAdminPort initialAdmin;
    private final ActiveEncryptionKeyPort activeKey;
    private final Clock clock;

    public SetupHandler(InstanceStatePort instanceState, SetupCodePort setupCodes, EnvironmentSettingsPort environment,
                        SettingsStorePort store, MailSettingsCheckPort mail, InitialAdminPort initialAdmin,
                        ActiveEncryptionKeyPort activeKey, Clock clock) {
        this.instanceState = instanceState;
        this.setupCodes = setupCodes;
        this.environment = environment;
        this.store = store;
        this.mail = mail;
        this.initialAdmin = initialAdmin;
        this.activeKey = activeKey;
        this.clock = clock;
    }

    @Override
    public SetupStatus status() {
        if (instanceState.load().setupCompleted()) {
            return SetupStatus.done();
        }
        EffectiveSettings settings = settings(store.load());
        return new SetupStatus(true,
            settings.locked(SettingKey.PUBLIC_URL) ? settings.text(SettingKey.PUBLIC_URL) : Optional.empty(),
            settings.locked(SettingKey.MAIL_HOST));
    }

    @Override
    public void verifyCode(String code) {
        pending(code);
    }

    @Override
    public SetupEncryptionKey encryptionKey(String code) {
        InstanceState state = pending(code);
        return state.keyGenerated() ? new SetupEncryptionKey(true, activeKey.value()) : new SetupEncryptionKey(false, null);
    }

    @Override
    public void sendTestMail(String code, Map<SettingKey, String> mailValues, String publicUrl, String recipient, Locale locale) {
        pending(code);
        Map<SettingKey, String> stored = store.load();
        EffectiveSettings current = settings(stored);
        if (current.locked(SettingKey.MAIL_HOST)) {
            throw new InstanceException.SettingLockedByEnvironment(SettingKey.MAIL_HOST);
        }
        SettingsDraft draft = draft(current, mailValues, publicUrl);
        if (!draft.problems().isEmpty()) {
            throw new InstanceException.SettingsInvalid(draft.problems());
        }
        EffectiveSettings tried = settings(draft.applyTo(stored));
        if (tried.text(SettingKey.MAIL_HOST).isEmpty()) {
            throw new InstanceException.SettingsInvalid(List.of(new SettingProblem(SettingKey.MAIL_HOST, SettingProblem.REQUIRED)));
        }
        mail.sendTest(MailDraft.of(tried), recipient, locale).ifPresent(failure -> {
            throw new InstanceException.MailTestFailed(failure.reason(), failure.serverReply());
        });
    }

    @Override
    @Transactional
    public UUID complete(CompleteSetupCommand command) {
        InstanceState state = pending(command.code());
        if (state.keyGenerated() && !command.encryptionKeySaved()) {
            throw new InstanceException.EncryptionKeyNotSaved();
        }
        Map<SettingKey, String> stored = store.load();
        EffectiveSettings current = settings(stored);
        boolean mailGiven = command.mail() != null && hasText(command.mail().get(SettingKey.MAIL_HOST));
        if (mailGiven && current.locked(SettingKey.MAIL_HOST)) {
            throw new InstanceException.SettingLockedByEnvironment(SettingKey.MAIL_HOST);
        }
        SettingsDraft draft = draft(current, mailGiven ? command.mail() : Map.of(), command.publicUrl());
        List<SettingProblem> problems = new ArrayList<>(draft.problems());
        if (problems.isEmpty()) {
            EffectiveSettings after = settings(draft.applyTo(stored));
            problems.addAll(SettingRules.crossProblems(after));
            if (mailGiven) {
                problems.addAll(mail.problems(MailDraft.of(after)));
            }
        }
        if (!problems.isEmpty()) {
            throw new InstanceException.SettingsInvalid(problems.stream().distinct().toList());
        }
        // First, and atomically: of two browsers finishing at the same second, one goes on, the other stops here.
        if (!instanceState.markSetupCompleted(clock.instant())) {
            throw new InstanceException.SetupAlreadyCompleted();
        }
        UUID adminId = initialAdmin.create(command.admin()).orElseThrow(() ->
            new IllegalStateException("The setup was pending, yet an account exists: nothing was changed"));
        store.save(draft.changes(), adminId, clock.instant());
        return adminId;
    }

    /** The address — unless the environment sets it — and the mail values typed in the wizard. */
    private static SettingsDraft draft(EffectiveSettings current, Map<SettingKey, String> mailValues, String publicUrl) {
        SettingsDraft draft = new SettingsDraft();
        if (!current.locked(SettingKey.PUBLIC_URL)) {
            if (hasText(publicUrl)) {
                draft.set(SettingKey.PUBLIC_URL, publicUrl);
            } else {
                draft.problem(new SettingProblem(SettingKey.PUBLIC_URL, SettingProblem.REQUIRED));
            }
        }
        mailValues.forEach((key, value) -> {
            if (key.group() != SettingGroup.MAIL) {
                throw new InstanceException.UnknownSetting(key.code());
            }
            draft.set(key, value);
        });
        return draft;
    }

    private InstanceState pending(String code) {
        InstanceState state = instanceState.load();
        if (state.setupCompleted()) {
            throw new InstanceException.SetupAlreadyCompleted();
        }
        if (setupCodes.current().filter(current -> current.matches(code)).isEmpty()) {
            throw new InstanceException.SetupCodeInvalid();
        }
        return state;
    }

    private EffectiveSettings settings(Map<SettingKey, String> stored) {
        return SettingsResolution.resolve(environment.values(), stored).settings();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
