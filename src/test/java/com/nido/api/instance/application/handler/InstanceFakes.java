package com.nido.api.instance.application.handler;

import com.nido.api.instance.domain.model.InitialAdmin;
import com.nido.api.instance.domain.model.InstanceState;
import com.nido.api.instance.domain.model.KeyFingerprint;
import com.nido.api.instance.domain.model.MailDraft;
import com.nido.api.instance.domain.model.MailTestFailure;
import com.nido.api.instance.domain.model.SettingKey;
import com.nido.api.instance.domain.model.SettingProblem;
import com.nido.api.instance.domain.model.SetupCode;
import com.nido.api.instance.domain.port.out.AccountRulesPort;
import com.nido.api.instance.domain.port.out.InitialAdminPort;
import com.nido.api.instance.domain.port.out.InstanceStatePort;
import com.nido.api.instance.domain.port.out.MailSettingsCheckPort;
import com.nido.api.instance.domain.port.out.SettingsStorePort;
import com.nido.api.instance.domain.port.out.SetupCodePort;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** In-memory stand-ins for the instance context's ports, shared by its handler tests. */
final class InstanceFakes {

    private InstanceFakes() {}

    static final class MemoryState implements InstanceStatePort {
        KeyFingerprint fingerprint;
        boolean fingerprintConfirmed = true;
        boolean keyGenerated;
        boolean setupCompleted;
        @Override public InstanceState load() {
            return new InstanceState(Optional.ofNullable(fingerprint), fingerprintConfirmed, keyGenerated, setupCompleted);
        }
        @Override public void recordFingerprint(KeyFingerprint f, boolean generated) {
            fingerprint = f;
            fingerprintConfirmed = false;
            keyGenerated = generated;
        }
        @Override public boolean confirmFingerprint(KeyFingerprint f) {
            boolean confirms = f.equals(fingerprint) && !fingerprintConfirmed;
            fingerprintConfirmed |= confirms;
            return confirms;
        }
        @Override public boolean forgetFingerprint(KeyFingerprint f) {
            if (!f.equals(fingerprint) || fingerprintConfirmed) {
                return false;
            }
            fingerprint = null;
            fingerprintConfirmed = true;
            keyGenerated = false;
            return true;
        }
        @Override public boolean markSetupCompleted(Instant at) { boolean was = setupCompleted; setupCompleted = true; return !was; }
    }

    static final class MemoryStore implements SettingsStorePort {
        final Map<SettingKey, String> rows = new EnumMap<>(SettingKey.class);
        UUID lastBy;
        @Override public Map<SettingKey, String> load() { return Map.copyOf(rows); }
        @Override public void save(Map<SettingKey, Optional<String>> changes, UUID by, Instant at) {
            lastBy = by;
            changes.forEach((key, value) -> value.ifPresentOrElse(v -> rows.put(key, v), () -> rows.remove(key)));
        }
    }

    static final class FakeMail implements MailSettingsCheckPort {
        List<SettingProblem> problems = List.of();
        Optional<MailTestFailure> failure = Optional.empty();
        MailDraft tried;
        @Override public List<SettingProblem> problems(MailDraft draft) { return problems; }
        @Override public Optional<MailTestFailure> sendTest(MailDraft draft, String recipient, Locale locale) { tried = draft; return failure; }
    }

    /** Every address and password hold, unless a test says what is wrong with them. */
    static final class FakeAccountRules implements AccountRulesPort {
        String emailProblem;
        String passwordProblem;

        @Override public Optional<String> emailProblem(String email) { return Optional.ofNullable(emailProblem); }
        @Override public Optional<String> passwordProblem(String password) { return Optional.ofNullable(passwordProblem); }
    }

    static final class MemoryCodes implements SetupCodePort {
        SetupCode code;
        @Override public void store(SetupCode code) { this.code = code; }
        @Override public Optional<SetupCode> current() { return Optional.ofNullable(code); }
    }

    static final class FakeAdmin implements InitialAdminPort {
        final List<InitialAdmin> created = new ArrayList<>();
        boolean usersExist;
        @Override public Optional<UUID> create(InitialAdmin admin) {
            if (usersExist) return Optional.empty();
            created.add(admin);
            usersExist = true;
            return Optional.of(UUID.randomUUID());
        }
    }
}
