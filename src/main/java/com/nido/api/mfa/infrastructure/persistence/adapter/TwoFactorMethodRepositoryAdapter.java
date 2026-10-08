package com.nido.api.mfa.infrastructure.persistence.adapter;

import com.nido.api.mfa.domain.model.UserTotpProfile;
import com.nido.api.mfa.domain.port.out.TwoFactorMethodStorePort;
import com.nido.api.mfa.domain.port.out.UserTotpLifecyclePort;
import com.nido.api.mfa.domain.port.out.UserTotpQueryPort;
import com.nido.api.mfa.infrastructure.config.TotpEncryptorFactory;
import com.nido.api.mfa.infrastructure.persistence.entity.TwoFactorMethodEntity;
import com.nido.api.mfa.infrastructure.persistence.repository.TwoFactorMethodJpaRepository;
import com.nido.api.shared.model.TwoFactorMethod;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TwoFactorMethodRepositoryAdapter implements TwoFactorMethodStorePort, UserTotpQueryPort, UserTotpLifecyclePort {

    private final TwoFactorMethodJpaRepository jpa;
    private final TotpEncryptorFactory encryptors;

    public TwoFactorMethodRepositoryAdapter(TwoFactorMethodJpaRepository jpa, TotpEncryptorFactory encryptors) {
        this.jpa = jpa;
        this.encryptors = encryptors;
    }

    @Override
    public Set<TwoFactorMethod> activeMethods(UUID userId) {
        Set<TwoFactorMethod> methods = EnumSet.noneOf(TwoFactorMethod.class);
        jpa.findByUserId(userId).forEach(row -> methods.add(row.getMethod()));
        return methods;
    }

    @Override
    public Map<UUID, Set<TwoFactorMethod>> activeMethodsAmong(Collection<UUID> userIds) {
        Map<UUID, Set<TwoFactorMethod>> methods = new HashMap<>();
        userIds.forEach(id -> methods.put(id, EnumSet.noneOf(TwoFactorMethod.class)));
        if (!userIds.isEmpty()) {
            jpa.findByUserIdIn(userIds).forEach(row -> methods.get(row.getUserId()).add(row.getMethod()));
        }
        return methods;
    }

    @Override
    public Optional<String> appSecret(UUID userId) {
        return jpa.findById(new TwoFactorMethodEntity.Key(userId, TwoFactorMethod.APP))
            .map(row -> encryptors.forUser(userId).decrypt(row.getSecret()));
    }

    @Override
    public void enable(UUID userId, TwoFactorMethod method, String secret) {
        String stored = method == TwoFactorMethod.APP
            ? encryptors.forUser(userId).encrypt(Objects.requireNonNull(secret, "secret"))
            : null;
        jpa.save(new TwoFactorMethodEntity(userId, method, stored));
    }

    @Override
    public boolean disable(UUID userId, TwoFactorMethod method) {
        return jpa.deleteMethod(userId, method) > 0;
    }

    @Override
    public void deleteAll(UUID userId) {
        jpa.deleteAllOf(userId);
    }

    // ── What the TOTP handlers still read, until they move behind TwoFactorMethodHandler (tasks 5 to 7) ──

    @Override
    public Optional<UserTotpProfile> findById(UUID userId) {
        Optional<String> secret = appSecret(userId);
        return Optional.of(new UserTotpProfile(userId, secret.isPresent(), secret));
    }

    @Override
    public Set<UUID> findTotpEnabledAmong(Collection<UUID> userIds) {
        return activeMethodsAmong(userIds).entrySet().stream()
            .filter(entry -> entry.getValue().contains(TwoFactorMethod.APP))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    @Override
    public void enableTotp(UUID userId, String secret) {
        enable(userId, TwoFactorMethod.APP, secret);
    }

    @Override
    public void disableTotp(UUID userId) {
        disable(userId, TwoFactorMethod.APP);
    }

    @Override
    public void deleteTotp(UUID userId) {
        deleteAll(userId);
    }
}
