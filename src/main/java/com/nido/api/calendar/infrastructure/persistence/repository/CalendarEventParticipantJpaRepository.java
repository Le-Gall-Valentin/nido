package com.nido.api.calendar.infrastructure.persistence.repository;

import com.nido.api.calendar.infrastructure.persistence.entity.CalendarEventParticipantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CalendarEventParticipantJpaRepository extends JpaRepository<CalendarEventParticipantEntity, UUID> {

    List<CalendarEventParticipantEntity> findByEventId(UUID eventId);

    /** Batched on purpose: a month window can return hundreds of events, and one query per event would be an N+1. */
    List<CalendarEventParticipantEntity> findByEventIdIn(Collection<UUID> eventIds);

    /**
     * One statement, not a check then an insert: two requests joining at once could both find the row
     * missing, and the second then failed on the unique constraint.
     */
    @Modifying
    @Query(value = """
        INSERT INTO calendar_event_participants (event_id, user_id) VALUES (:eventId, :userId)
        ON CONFLICT (event_id, user_id) DO NOTHING
        """, nativeQuery = true)
    void insertIfAbsent(@Param("eventId") UUID eventId, @Param("userId") UUID userId);

    void deleteByEventId(UUID eventId);

    void deleteByEventIdAndUserId(UUID eventId, UUID userId);
}
