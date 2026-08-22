package com.nido.api.space.infrastructure.persistence.repository;

import com.nido.api.space.infrastructure.persistence.entity.SpaceInvitationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpaceInvitationJpaRepository extends JpaRepository<SpaceInvitationEntity, UUID> {

    Optional<SpaceInvitationEntity> findByCode(String code);

    List<SpaceInvitationEntity> findBySpaceIdOrderByCreatedAtDesc(UUID spaceId);

    @Query("""
        select i from SpaceInvitationEntity i
        where lower(i.email) = lower(:email)
          and i.status = com.nido.api.space.domain.model.InvitationStatus.PENDING
          and i.expiresAt > :now
        order by i.createdAt desc
        """)
    List<SpaceInvitationEntity> findPendingForEmail(@Param("email") String email, @Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Query("""
        update SpaceInvitationEntity i
        set i.status = com.nido.api.space.domain.model.InvitationStatus.ACCEPTED, i.acceptedAt = :acceptedAt
        where i.id = :id and i.status = com.nido.api.space.domain.model.InvitationStatus.PENDING
        """)
    int claim(@Param("id") UUID id, @Param("acceptedAt") Instant acceptedAt);

    @Modifying(clearAutomatically = true)
    @Query("""
        update SpaceInvitationEntity i
        set i.status = com.nido.api.space.domain.model.InvitationStatus.REVOKED
        where i.id = :id and i.status = com.nido.api.space.domain.model.InvitationStatus.PENDING
        """)
    int revoke(@Param("id") UUID id);
}
