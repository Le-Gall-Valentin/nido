package com.nido.api.calendar.infrastructure.persistence.repository;

import com.nido.api.calendar.infrastructure.persistence.entity.CalendarRecurringEventSeriesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CalendarRecurringEventSeriesJpaRepository
    extends JpaRepository<CalendarRecurringEventSeriesEntity, UUID> {

    List<CalendarRecurringEventSeriesEntity> findBySpaceIdOrderByAnchorDate(UUID spaceId);
}
