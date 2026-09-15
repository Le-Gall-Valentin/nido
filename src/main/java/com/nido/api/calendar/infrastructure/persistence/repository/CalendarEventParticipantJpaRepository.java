package com.nido.api.calendar.infrastructure.persistence.repository;

import com.nido.api.calendar.infrastructure.persistence.entity.CalendarEventParticipantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CalendarEventParticipantJpaRepository extends JpaRepository<CalendarEventParticipantEntity, UUID> {

    List<CalendarEventParticipantEntity> findByEventId(UUID eventId);

    /** Batched on purpose: a month window can return hundreds of events, and one query per event would be an N+1. */
    List<CalendarEventParticipantEntity> findByEventIdIn(Collection<UUID> eventIds);

    boolean existsByEventIdAndUserId(UUID eventId, UUID userId);

    void deleteByEventId(UUID eventId);

    void deleteByEventIdAndUserId(UUID eventId, UUID userId);
}
