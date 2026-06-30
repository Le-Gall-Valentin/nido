package com.synapsys.api.mfa.infrastructure.persistence.repository;

import com.synapsys.api.mfa.infrastructure.persistence.entity.UserTotpEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface UserTotpJpaRepository extends JpaRepository<UserTotpEntity, UUID> {

    @Modifying(clearAutomatically = true) @Transactional
    @Query("UPDATE UserTotpEntity u SET u.totpSecret = :secret WHERE u.userId = :id AND u.totpSecret IS NULL")
    int saveTotpSecretIfAbsent(@Param("id") UUID id, @Param("secret") String secret);

    @Modifying(clearAutomatically = true) @Transactional
    @Query("UPDATE UserTotpEntity u SET u.totpSecret = null WHERE u.userId = :id AND u.totpEnabled = false")
    void clearPendingSecretById(@Param("id") UUID id);

    @Modifying(clearAutomatically = true) @Transactional
    @Query("UPDATE UserTotpEntity u SET u.totpEnabled = true WHERE u.userId = :id")
    void enableTotpById(@Param("id") UUID id);

    @Modifying(clearAutomatically = true) @Transactional
    @Query("UPDATE UserTotpEntity u SET u.totpEnabled = false, u.totpSecret = null WHERE u.userId = :id")
    void disableTotpById(@Param("id") UUID id);

    @Query("SELECT u.userId FROM UserTotpEntity u WHERE u.userId IN :ids AND u.totpEnabled = true")
    Set<UUID> findTotpEnabledIds(@Param("ids") Collection<UUID> ids);
}