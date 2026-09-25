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

    boolean existsBySeriesIdAndOriginalDate(UUID seriesId, LocalDate originalDate);

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
