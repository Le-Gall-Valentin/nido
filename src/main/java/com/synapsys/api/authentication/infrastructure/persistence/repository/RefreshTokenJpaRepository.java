package com.synapsys.api.authentication.infrastructure.persistence.repository;

import com.synapsys.api.authentication.infrastructure.persistence.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshTokenEntity t SET t.revoked = true, t.lastUsedAt = :now WHERE t.id = :id AND t.revoked = false")
    int markUsedAndRevokeIfNotRevokedById(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshTokenEntity t SET t.revoked = true WHERE t.id = :id")
    void revokeById(UUID id);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshTokenEntity t SET t.revoked = true WHERE t.userId = :userId")
    void revokeAllByUserId(UUID userId);

    @Modifying
    @Transactional
    void deleteByUserId(UUID userId);

    // Removes: (1) all expired tokens; (2) revoked tokens last used before cutoff; (3) revoked tokens never used but created before cutoff
    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshTokenEntity t WHERE t.expiresAt < :now OR (t.revoked = true AND (t.lastUsedAt < :cutoff OR (t.lastUsedAt IS NULL AND t.createdAt < :cutoff)))")
    int deleteExpiredAndOldRevoked(@Param("now") Instant now, @Param("cutoff") Instant cutoff);
}