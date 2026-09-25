package com.nido.api.calendar.infrastructure.persistence.repository;

import com.nido.api.calendar.infrastructure.persistence.entity.CalendarRecurringEventSeriesParticipantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CalendarRecurringEventSeriesParticipantJpaRepository
    extends JpaRepository<CalendarRecurringEventSeriesParticipantEntity, UUID> {

    List<CalendarRecurringEventSeriesParticipantEntity> findBySeriesId(UUID seriesId);

    /** Batched for the same N+1 reason as the event participants. */
    List<CalendarRecurringEventSeriesParticipantEntity> findBySeriesIdIn(Collection<UUID> seriesIds);

    void deleteBySeriesId(UUID seriesId);
}
