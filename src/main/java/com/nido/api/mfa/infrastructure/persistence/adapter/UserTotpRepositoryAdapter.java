package com.nido.api.mfa.infrastructure.persistence.adapter;

import com.nido.api.mfa.domain.model.UserTotpProfile;
import com.nido.api.mfa.domain.port.out.UserTotpInitPort;
import com.nido.api.mfa.domain.port.out.UserTotpLifecyclePort;
import com.nido.api.mfa.domain.port.out.UserTotpQueryPort;
import com.nido.api.mfa.infrastructure.config.TotpEncryptorFactory;
import com.nido.api.mfa.infrastructure.persistence.entity.UserTotpEntity;
import com.nido.api.mfa.infrastructure.persistence.repository.UserTotpJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class UserTotpRepositoryAdapter
        implements UserTotpInitPort, UserTotpLifecyclePort, UserTotpQueryPort {

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
    public void enableTotp(UUID userId, String secret) {
        jpa.enableTotpById(userId, encryptorFactory.forUser(userId).encrypt(secret));
    }

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