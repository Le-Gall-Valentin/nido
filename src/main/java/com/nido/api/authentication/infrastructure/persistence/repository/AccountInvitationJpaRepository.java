package com.nido.api.authentication.infrastructure.persistence.repository;

import com.nido.api.authentication.infrastructure.persistence.entity.AccountInvitationEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountInvitationJpaRepository extends JpaRepository<AccountInvitationEntity, UUID> {

    Optional<AccountInvitationEntity> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM AccountInvitationEntity i WHERE i.tokenHash = :tokenHash")
    Optional<AccountInvitationEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    List<AccountInvitationEntity> findByUserIdIn(Collection<UUID> userIds);
}
