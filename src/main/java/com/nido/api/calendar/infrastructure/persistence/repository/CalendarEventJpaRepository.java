package com.nido.api.calendar.infrastructure.persistence.repository;

import com.nido.api.calendar.infrastructure.persistence.entity.CalendarEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CalendarEventJpaRepository extends JpaRepository<CalendarEventEntity, UUID> {

    /**
     * Overlap, not containment: {@code startDate <= to AND endDate >= from}. A plain
     * {@code startDate BETWEEN from AND to} would silently drop a fortnight's holiday from
     * every week it spans except the first.
     */
    @Query("""
        SELECT e FROM CalendarEventEntity e
        WHERE e.spaceId = :spaceId AND e.startDate <= :to AND e.endDate >= :from
        ORDER BY e.startDate, e.startTime
        """)
    List<CalendarEventEntity> findOverlapping(
        @Param("spaceId") UUID spaceId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Queried on the ORIGINAL date, so a slot stays freed even when its instance moved away. */
    @Query("""
        SELECT e.recurringOriginalDate FROM CalendarEventEntity e
        WHERE e.recurringSeriesId = :seriesId AND e.recurringOriginalDate BETWEEN :from AND :to
        """)
    List<LocalDate> findDetachedSlots(
        @Param("seriesId") UUID seriesId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    Optional<CalendarEventEntity> findByRecurringSeriesIdAndRecurringOriginalDate(UUID seriesId, LocalDate originalDate);
}
