package com.nido.api.calendar.infrastructure.persistence.repository;

import com.nido.api.calendar.infrastructure.persistence.entity.CalendarEventExclusionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CalendarEventExclusionJpaRepository extends JpaRepository<CalendarEventExclusionEntity, UUID> {

    List<CalendarEventExclusionEntity> findBySeriesIdAndOriginalDateBetween(
        UUID seriesId, LocalDate from, LocalDate to);

    boolean existsBySeriesIdAndOriginalDate(UUID seriesId, LocalDate originalDate);

    void deleteBySeriesIdAndOriginalDate(UUID seriesId, LocalDate originalDate);
}
