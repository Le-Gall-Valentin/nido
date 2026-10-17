package com.nido.api.calendar.infrastructure.persistence.repository;

import com.nido.api.calendar.infrastructure.persistence.entity.CalendarExternalFeedEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CalendarExternalFeedJpaRepository extends JpaRepository<CalendarExternalFeedEntity, UUID> {
    List<CalendarExternalFeedEntity> findBySpaceIdOrderByCreatedAt(UUID spaceId);

    long countBySpaceId(UUID spaceId);

    List<CalendarExternalFeedEntity> findByLastAttemptAtBefore(Instant before);
}
