package com.nido.api.calendar.infrastructure.persistence.repository;

import com.nido.api.calendar.infrastructure.persistence.entity.CalendarSubscriptionLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CalendarSubscriptionLinkJpaRepository extends JpaRepository<CalendarSubscriptionLinkEntity, UUID> {

    List<CalendarSubscriptionLinkEntity> findBySpaceIdAndUserIdOrderByCreatedAt(UUID spaceId, UUID userId);

    Optional<CalendarSubscriptionLinkEntity> findByTokenHash(String tokenHash);

    long countBySpaceIdAndUserId(UUID spaceId, UUID userId);

    @Modifying
    @Query("DELETE FROM CalendarSubscriptionLinkEntity l WHERE l.id = :id AND l.spaceId = :spaceId AND l.userId = :userId")
    int deleteOwned(@Param("id") UUID id, @Param("spaceId") UUID spaceId, @Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM CalendarSubscriptionLinkEntity l WHERE l.spaceId = :spaceId AND l.userId = :userId")
    void deleteBySpaceAndUser(@Param("spaceId") UUID spaceId, @Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE CalendarSubscriptionLinkEntity l SET l.lastReadAt = :at WHERE l.id = :id")
    void markRead(@Param("id") UUID id, @Param("at") Instant at);
}
