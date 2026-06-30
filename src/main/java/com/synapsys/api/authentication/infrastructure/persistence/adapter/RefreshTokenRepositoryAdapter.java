package com.synapsys.api.authentication.infrastructure.persistence.adapter;

import com.synapsys.api.authentication.domain.model.RefreshToken;
import com.synapsys.api.authentication.domain.port.out.RefreshTokenMaintenancePort;
import com.synapsys.api.authentication.domain.port.out.RefreshTokenRepository;
import com.synapsys.api.authentication.domain.port.out.RefreshTokenRevocationPort;
import com.synapsys.api.authentication.infrastructure.persistence.entity.RefreshTokenEntity;
import com.synapsys.api.authentication.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository, RefreshTokenRevocationPort, RefreshTokenMaintenancePort {

    private final RefreshTokenJpaRepository jpa;

    public RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    public boolean tryMarkUsedAndRevoke(UUID tokenId) {
        return jpa.markUsedAndRevokeIfNotRevokedById(tokenId, Instant.now()) > 0;
    }

    @Override
    public void revoke(UUID tokenId) {
        jpa.revokeById(tokenId);
    }

    // REQUIRES_NEW: revocation must commit independently so tokens stay revoked
    // even if the caller's outer transaction is rolled back (e.g., on reuse detection).
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeAllForUser(UUID userId) {
        jpa.revokeAllByUserId(userId);
    }

    // REQUIRED propagation (default): must participate in the GDPR delete transaction.
    // Do NOT change to REQUIRES_NEW — unlike revokeAllForUser, token deletion here
    // must roll back if identity anonymization fails.
    @Override
    public void deleteAllForUser(UUID userId) {
        jpa.deleteByUserId(userId);
    }

    @Override
    public int deleteExpiredAndRevoked(Instant now, Instant cutoff) {
        return jpa.deleteExpiredAndOldRevoked(now, cutoff);
    }

    @Override
    public RefreshToken save(UUID userId, String tokenHash, Instant expiresAt) {
        RefreshTokenEntity entity = new RefreshTokenEntity(userId, tokenHash, expiresAt);
        RefreshTokenEntity saved = jpa.save(entity);
        return toDomain(saved);
    }

    private RefreshToken toDomain(RefreshTokenEntity e) {
        return new RefreshToken(e.getId(), e.getUserId(), e.getTokenHash(),
            e.getExpiresAt(), e.isRevoked(), e.getCreatedAt(), e.getLastUsedAt());
    }
}