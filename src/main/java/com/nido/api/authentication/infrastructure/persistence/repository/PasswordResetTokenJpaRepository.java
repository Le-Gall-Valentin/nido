package com.nido.api.authentication.infrastructure.persistence.repository;

import com.nido.api.authentication.infrastructure.persistence.entity.PasswordResetTokenEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {

    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM PasswordResetTokenEntity t WHERE t.tokenHash = :tokenHash")
    Optional<PasswordResetTokenEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Query("SELECT max(t.createdAt) FROM PasswordResetTokenEntity t WHERE t.userId = :userId")
    Optional<Instant> findLatestCreatedAt(@Param("userId") UUID userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetTokenEntity t WHERE t.userId = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetTokenEntity t WHERE t.expiresAt <= :now")
    int deleteExpired(@Param("now") Instant now);
}
