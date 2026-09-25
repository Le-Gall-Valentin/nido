package com.nido.api.calendar.infrastructure.persistence.repository;

import com.nido.api.calendar.infrastructure.persistence.entity.CalendarEventExclusionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CalendarEventExclusionJpaRepository extends JpaRepository<CalendarEventExclusionEntity, UUID> {

    /** One statement, for the same reason as a participant: two cancellations at once must not collide. */
    @Modifying
    @Query(value = """
        INSERT INTO calendar_event_exclusions (series_id, original_date) VALUES (:seriesId, :originalDate)
        ON CONFLICT (series_id, original_date) DO NOTHING
        """, nativeQuery = true)
    void insertIfAbsent(@Param("seriesId") UUID seriesId, @Param("originalDate") LocalDate originalDate);

    void deleteBySeriesIdAndOriginalDate(UUID seriesId, LocalDate originalDate);

    List<CalendarEventExclusionEntity> findBySeriesId(UUID seriesId);

    List<CalendarEventExclusionEntity> findBySeriesIdInAndOriginalDateBetween(
        Collection<UUID> seriesIds, LocalDate from, LocalDate to);

    /** Run at once, not at the next flush: the same slots may be cancelled again right after. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM CalendarEventExclusionEntity x WHERE x.seriesId = :seriesId AND x.originalDate >= :from")
    void deleteFrom(@Param("seriesId") UUID seriesId, @Param("from") LocalDate from);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM CalendarEventExclusionEntity x WHERE x.seriesId = :seriesId")
    void deleteAllOf(@Param("seriesId") UUID seriesId);
}
