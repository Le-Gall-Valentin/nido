package com.nido.api.calendar.infrastructure.persistence.repository;

import com.nido.api.calendar.infrastructure.persistence.entity.CalendarRecurringEventSeriesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CalendarRecurringEventSeriesJpaRepository
    extends JpaRepository<CalendarRecurringEventSeriesEntity, UUID> {

    List<CalendarRecurringEventSeriesEntity> findBySpaceIdOrderByAnchorDate(UUID spaceId);

    @Query("SELECT DISTINCT e.importUidHash FROM CalendarRecurringEventSeriesEntity e WHERE e.spaceId = :spaceId AND e.importUidHash IN :hashes")
    List<String> findImportHashes(@Param("spaceId") UUID spaceId, @Param("hashes") Collection<String> hashes);

    @Query("SELECT e.id FROM CalendarRecurringEventSeriesEntity e WHERE e.spaceId = :spaceId AND e.id IN :ids")
    List<UUID> findIdsIn(@Param("spaceId") UUID spaceId, @Param("ids") Collection<UUID> ids);
}
