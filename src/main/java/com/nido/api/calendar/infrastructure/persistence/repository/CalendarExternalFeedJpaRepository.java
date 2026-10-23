package com.nido.api.calendar.infrastructure.persistence.repository;

import com.nido.api.calendar.domain.model.FeedToShow;
import com.nido.api.calendar.infrastructure.persistence.entity.CalendarExternalFeedEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CalendarExternalFeedJpaRepository extends JpaRepository<CalendarExternalFeedEntity, UUID> {
    List<CalendarExternalFeedEntity> findBySpaceIdOrderByCreatedAt(UUID spaceId);

    long countBySpaceId(UUID spaceId);

    @Query("select f.id from CalendarExternalFeedEntity f where f.lastAttemptAt < :before order by f.lastAttemptAt asc, f.id asc")
    List<UUID> findIdsAttemptedBefore(@Param("before") Instant before, Pageable pageable);

    @Query("select new com.nido.api.calendar.domain.model.FeedToShow(f.id, f.color, f.contentFetchedAt)"
        + " from CalendarExternalFeedEntity f where f.spaceId = :spaceId order by f.createdAt")
    List<FeedToShow> findShownBySpaceId(@Param("spaceId") UUID spaceId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update CalendarExternalFeedEntity f set f.lastAttemptAt = :at where f.id = :id and f.lastAttemptAt < :before")
    int claimAttempt(@Param("id") UUID id, @Param("at") Instant at, @Param("before") Instant before);
}
