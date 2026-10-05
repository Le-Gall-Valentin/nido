package com.nido.api.instance.application.handler;

import com.nido.api.instance.application.port.in.StartInstanceUseCase;
import com.nido.api.instance.domain.model.EffectiveSettings;
import com.nido.api.instance.domain.model.EnvironmentSeed;
import com.nido.api.instance.domain.model.InitialAdmin;
import com.nido.api.instance.domain.model.InstanceException;
import com.nido.api.instance.domain.model.InstanceState;
import com.nido.api.instance.domain.model.MailDraft;
import com.nido.api.instance.domain.model.SettingKey;
import com.nido.api.instance.domain.model.SettingProblem;
import com.nido.api.instance.domain.model.SettingRules;
import com.nido.api.instance.domain.model.SettingsResolution;
import com.nido.api.instance.domain.model.SetupCode;
import com.nido.api.instance.domain.port.out.AccountRulesPort;
import com.nido.api.instance.domain.port.out.EnvironmentSettingsPort;
import com.nido.api.instance.domain.port.out.InitialAdminPort;
import com.nido.api.instance.domain.port.out.InstanceStatePort;
import com.nido.api.instance.domain.port.out.KeyFilePort;
import com.nido.api.instance.domain.port.out.MailSettingsCheckPort;
import com.nido.api.instance.domain.port.out.SettingsStorePort;
import com.nido.api.instance.domain.port.out.SetupCodePort;
import com.nido.api.shared.annotation.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationService
public class StartInstanceHandler implements StartInstanceUseCase {

    private static final Logger log = LoggerFactory.getLogger(StartInstanceHandler.class);

    private final EnvironmentSettingsPort environment;
    private final SettingsStorePort store;
    private final MailSettingsCheckPort mail;
    private final InstanceStatePort instanceState;
    private final InitialAdminPort initialAdmin;
    private final SetupCodePort setupCodes;
    private final KeyFilePort keyFile;
    private final AccountRulesPort accountRules;
    private final Clock clock;

    public StartInstanceHandler(EnvironmentSettingsPort environment, SettingsStorePort store, MailSettingsCheckPort mail,
                                InstanceStatePort instanceState, InitialAdminPort initialAdmin, SetupCodePort setupCodes,
                                KeyFilePort keyFile, AccountRulesPort accountRules, Clock clock) {
        this.environment = environment;
        this.store = store;
        this.mail = mail;
        this.instanceState = instanceState;
        this.initialAdmin = initialAdmin;
        this.setupCodes = setupCodes;
        this.keyFile = keyFile;
        this.accountRules = accountRules;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Optional<SetupCode> start(EnvironmentSeed seed) {
        checkEnvironment();
        InstanceState state = instanceState.load();
        if (state.setupCompleted()) {
            if (seed.anyGiven()) {
                log.info("NIDO_SEED_* are ignored: this installation is set up. You can remove them.");
            }
            return Optional.empty();
        }
        if (seed.allGiven()) {
            seedFromEnvironment(seed, state);
            return Optional.empty();
        }
        if (seed.anyGiven()) {
            throw new IllegalStateException("NIDO_SEED_USERNAME, NIDO_SEED_EMAIL and NIDO_SEED_PASSWORD go together: "
                + "set all three, or none to set Nido up from its setup screen");
        }
        SetupCode code = SetupCode.generate();
        setupCodes.store(code);
        return Optional.of(code);
    }

    /**
     * Held to the rules of the setup screen, which this account skips — checked here, once the seed is
     * known to be used: an installation already set up ignores its old NIDO_SEED_*, whatever they say.
     */
    private void seedFromEnvironment(EnvironmentSeed seed, InstanceState state) {
        List<String> problems = new ArrayList<>();
        accountRules.emailProblem(seed.email().strip()).ifPresent(problem -> problems.add("NIDO_SEED_EMAIL " + problem));
        accountRules.passwordProblem(seed.password()).ifPresent(problem -> problems.add("NIDO_SEED_PASSWORD " + problem));
        if (!problems.isEmpty()) {
            throw new IllegalStateException("The first administrator in the environment is not valid, so Nido does not start: "
                + String.join("; ", problems));
        }
        instanceState.markSetupCompleted(clock.instant());
        try {
            initialAdmin.create(new InitialAdmin(seed.username(), seed.email(), seed.password(), null))
                .orElseThrow(() -> new IllegalStateException("The setup was pending, yet an account exists"));
        } catch (InstanceException.InitialAdminRefused refused) {
            throw new IllegalStateException("NIDO_SEED_USERNAME is not a username Nido accepts: 3 to 50 characters, no @");
        }
        log.info("Initial SUPER_ADMIN '{}' created from NIDO_SEED_*", seed.username());
        if (state.keyGenerated()) {
            log.warn("The encryption key was generated in {} and this installation skipped the setup screen that shows "
                + "it: back that file up, away from your database dumps.", keyFile.location());
        }
    }

    /** A wrong value in the environment stops the start — the variable is named, never its value. */
    private void checkEnvironment() {
        Map<SettingKey, String> variables = environment.values();
        List<String> problems = new ArrayList<>();
        variables.forEach((key, value) -> SettingRules.problem(key, value)
            .ifPresent(code -> problems.add(key.variable() + ": " + code)));
        if (problems.isEmpty()) {
            EffectiveSettings settings = SettingsResolution.resolve(variables, store.load()).settings();
            for (SettingProblem problem : SettingRules.crossProblems(settings)) {
                boolean fromEnvironment = switch (problem.code()) {
                    case SettingProblem.ACCESS_NOT_SHORTER_THAN_SESSION ->
                        settings.locked(SettingKey.ACCESS_TOKEN_MINUTES) || settings.locked(SettingKey.REFRESH_TOKEN_DAYS);
                    case SettingProblem.PUBLIC_URL_REQUIRED_BY_MAIL -> settings.locked(SettingKey.MAIL_HOST);
                    default -> settings.locked(problem.key());
                };
                if (fromEnvironment) {
                    problems.add(problem.key().variable() + ": " + problem.code());
                }
            }
            if (settings.locked(SettingKey.MAIL_HOST) && settings.text(SettingKey.MAIL_HOST).isPresent()) {
                mail.problems(MailDraft.of(settings)).forEach(problem -> problems.add(problem.key().variable() + ": " + problem.code()));
            }
        }
        if (!problems.isEmpty()) {
            throw new IllegalStateException("The configuration in the environment is not valid, so Nido does not start: "
                + String.join("; ", problems.stream().distinct().toList()));
        }
    }
}
