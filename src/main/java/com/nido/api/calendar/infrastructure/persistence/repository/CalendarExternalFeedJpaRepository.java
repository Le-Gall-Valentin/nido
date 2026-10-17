package com.nido.api.calendar.infrastructure.persistence.repository;

import com.nido.api.calendar.infrastructure.persistence.entity.CalendarExternalFeedEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CalendarExternalFeedJpaRepository extends JpaRepository<CalendarExternalFeedEntity, UUID> {
    List<CalendarExternalFeedEntity> findBySpaceIdOrderByCreatedAt(UUID spaceId);

    long countBySpaceId(UUID spaceId);

    @Query("select f.id from CalendarExternalFeedEntity f where f.lastAttemptAt < :before order by f.lastAttemptAt asc")
    List<UUID> findIdsAttemptedBefore(@Param("before") Instant before, Pageable pageable);
}
