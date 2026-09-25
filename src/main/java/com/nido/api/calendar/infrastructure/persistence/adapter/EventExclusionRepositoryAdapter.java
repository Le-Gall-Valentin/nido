package com.nido.api.calendar.infrastructure.persistence.adapter;

import com.nido.api.calendar.domain.port.out.EventExclusionRepository;
import com.nido.api.calendar.infrastructure.persistence.entity.CalendarEventExclusionEntity;
import com.nido.api.calendar.infrastructure.persistence.repository.CalendarEventExclusionJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class EventExclusionRepositoryAdapter implements EventExclusionRepository {

    private final CalendarEventExclusionJpaRepository exclusions;

    public EventExclusionRepositoryAdapter(CalendarEventExclusionJpaRepository exclusions) {
        this.exclusions = exclusions;
    }

    @Override
    public Set<LocalDate> findSlots(UUID seriesId, LocalDate from, LocalDate to) {
        return exclusions.findBySeriesIdAndOriginalDateBetween(seriesId, from, to).stream()
            .map(CalendarEventExclusionEntity::getOriginalDate)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public Set<LocalDate> findAllSlots(UUID seriesId) {
        return exclusions.findBySeriesId(seriesId).stream()
            .map(CalendarEventExclusionEntity::getOriginalDate)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    @Transactional
    public void clearFrom(UUID seriesId, LocalDate from) {
        if (from == null) {
            exclusions.deleteAllOf(seriesId);
        } else {
            exclusions.deleteFrom(seriesId, from);
        }
    }

    @Override
    @Transactional
    public void exclude(UUID seriesId, LocalDate originalDate) {
        if (exclusions.existsBySeriesIdAndOriginalDate(seriesId, originalDate)) {
            return;
        }
        CalendarEventExclusionEntity row = new CalendarEventExclusionEntity();
        row.setSeriesId(seriesId);
        row.setOriginalDate(originalDate);
        exclusions.save(row);
    }

    @Override
    @Transactional
    public void clear(UUID seriesId, LocalDate originalDate) {
        exclusions.deleteBySeriesIdAndOriginalDate(seriesId, originalDate);
    }
}
