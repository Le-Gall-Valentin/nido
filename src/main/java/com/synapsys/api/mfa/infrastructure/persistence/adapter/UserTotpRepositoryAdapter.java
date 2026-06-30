package com.synapsys.api.mfa.infrastructure.persistence.adapter;

import com.synapsys.api.mfa.domain.model.UserTotpProfile;
import com.synapsys.api.mfa.domain.port.out.UserTotpInitPort;
import com.synapsys.api.mfa.domain.port.out.UserTotpLifecyclePort;
import com.synapsys.api.mfa.domain.port.out.UserTotpQueryPort;
import com.synapsys.api.mfa.domain.port.out.UserTotpSetupPort;
import com.synapsys.api.mfa.infrastructure.config.TotpEncryptorFactory;
import com.synapsys.api.mfa.infrastructure.persistence.entity.UserTotpEntity;
import com.synapsys.api.mfa.infrastructure.persistence.repository.UserTotpJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class UserTotpRepositoryAdapter
        implements UserTotpInitPort, UserTotpSetupPort, UserTotpLifecyclePort, UserTotpQueryPort {

    private final UserTotpJpaRepository jpa;
    private final TotpEncryptorFactory encryptorFactory;

    public UserTotpRepositoryAdapter(UserTotpJpaRepository jpa,
                                     TotpEncryptorFactory encryptorFactory) {
        this.jpa = jpa;
        this.encryptorFactory = encryptorFactory;
    }

    @Override
    public Optional<UserTotpProfile> findById(UUID userId) {
        return jpa.findById(userId).map(e -> {
            Optional<String> secret = Optional.ofNullable(e.getTotpSecret())
                .map(enc -> encryptorFactory.forUser(userId).decrypt(enc));
            return new UserTotpProfile(userId, e.isTotpEnabled(), secret);
        });
    }

    @Override
    public void createDefaultRecord(UUID userId) {
        try {
            UserTotpEntity e = new UserTotpEntity();
            e.setUserId(userId);
            jpa.save(e);
        } catch (DataIntegrityViolationException ignored) {
            // Concurrent call already created the record — idempotent
        }
    }

    @Override
    public boolean saveTotpSecretIfAbsent(UUID userId, String secret) {
        return jpa.saveTotpSecretIfAbsent(userId, encryptorFactory.forUser(userId).encrypt(secret)) > 0;
    }

    @Override
    public void clearPendingSecret(UUID userId) {
        jpa.clearPendingSecretById(userId);
    }

    @Override
    public void enableTotp(UUID userId) { jpa.enableTotpById(userId); }

    @Override
    public void disableTotp(UUID userId) { jpa.disableTotpById(userId); }

    @Override
    public void deleteTotp(UUID userId) { jpa.deleteById(userId); }

    @Override
    public Set<UUID> findTotpEnabledAmong(Collection<UUID> userIds) {
        if (userIds.isEmpty()) return Set.of();
        return jpa.findTotpEnabledIds(userIds);
    }
}