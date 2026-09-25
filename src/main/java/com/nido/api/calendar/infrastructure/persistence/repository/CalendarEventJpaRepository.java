package com.nido.api.calendar.infrastructure.persistence.repository;

import com.nido.api.calendar.infrastructure.persistence.entity.CalendarEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
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

    Optional<CalendarEventEntity> findByRecurringSeriesIdAndRecurringOriginalDate(UUID seriesId, LocalDate originalDate);

    List<CalendarEventEntity> findByRecurringSeriesId(UUID seriesId);

    /**
     * The slot each detached instance takes over, series by series. Queried on the ORIGINAL date, so a
     * slot stays freed even when its instance moved away; and on the two columns, not the whole rows.
     */
    @Query("""
        SELECT e.recurringSeriesId AS seriesId, e.recurringOriginalDate AS originalDate FROM CalendarEventEntity e
        WHERE e.recurringSeriesId IN :seriesIds AND e.recurringOriginalDate BETWEEN :from AND :to
        """)
    List<SeriesSlot> findDetachedSlotsOfEach(
        @Param("seriesIds") Collection<UUID> seriesIds, @Param("from") LocalDate from, @Param("to") LocalDate to);

    interface SeriesSlot {
        UUID getSeriesId();

        LocalDate getOriginalDate();
    }

    /** Run at once, not at the next flush: the next relink may claim the slot this one frees. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        UPDATE CalendarEventEntity e SET e.recurringSeriesId = :seriesId, e.recurringOriginalDate = :originalDate
        WHERE e.id = :eventId
        """)
    void relink(@Param("eventId") UUID eventId, @Param("seriesId") UUID seriesId,
                @Param("originalDate") LocalDate originalDate);
}
