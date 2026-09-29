package com.nido.api.authentication.infrastructure.persistence.adapter;

import com.nido.api.authentication.domain.model.PasswordResetToken;
import com.nido.api.authentication.domain.port.out.PasswordResetTokenRepository;
import com.nido.api.authentication.infrastructure.persistence.entity.PasswordResetTokenEntity;
import com.nido.api.authentication.infrastructure.persistence.repository.PasswordResetTokenJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class PasswordResetTokenRepositoryAdapter implements PasswordResetTokenRepository {

    private final PasswordResetTokenJpaRepository jpa;

    public PasswordResetTokenRepositoryAdapter(PasswordResetTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(UUID userId, String tokenHash, Instant createdAt, Instant expiresAt) {
        jpa.save(new PasswordResetTokenEntity(userId, tokenHash, createdAt, expiresAt));
    }

    @Override
    public Optional<PasswordResetToken> findByHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(PasswordResetTokenRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional
    public Optional<PasswordResetToken> consumeByHash(String tokenHash) {
        Optional<PasswordResetTokenEntity> found = jpa.findByTokenHashForUpdate(tokenHash);
        found.ifPresent(jpa::delete);
        return found.map(PasswordResetTokenRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<Instant> latestIssuedAt(UUID userId) {
        return jpa.findLatestCreatedAt(userId);
    }

    @Override
    public void deleteAllForUser(UUID userId) {
        jpa.deleteAllByUserId(userId);
    }

    @Override
    public int deleteExpired(Instant now) {
        return jpa.deleteExpired(now);
    }

    private static PasswordResetToken toDomain(PasswordResetTokenEntity entity) {
        return new PasswordResetToken(entity.getId(), entity.getUserId(), entity.getCreatedAt(), entity.getExpiresAt());
    }
}
