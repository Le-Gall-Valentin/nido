package com.nido.api.mfa.infrastructure.persistence.repository;

import com.nido.api.mfa.infrastructure.persistence.entity.TwoFactorMethodEntity;
import com.nido.api.shared.model.TwoFactorMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TwoFactorMethodJpaRepository extends JpaRepository<TwoFactorMethodEntity, TwoFactorMethodEntity.Key> {

    List<TwoFactorMethodEntity> findByUserId(UUID userId);

    List<TwoFactorMethodEntity> findByUserIdIn(Collection<UUID> userIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("DELETE FROM TwoFactorMethodEntity m WHERE m.userId = :userId AND m.method = :method")
    int deleteMethod(@Param("userId") UUID userId, @Param("method") TwoFactorMethod method);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("DELETE FROM TwoFactorMethodEntity m WHERE m.userId = :userId")
    int deleteAllOf(@Param("userId") UUID userId);
}
