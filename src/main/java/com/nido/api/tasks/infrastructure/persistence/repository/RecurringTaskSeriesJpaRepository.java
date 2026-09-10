package com.nido.api.tasks.infrastructure.persistence.repository;

import com.nido.api.tasks.domain.model.RecurringTaskSeriesSchedule;
import com.nido.api.tasks.infrastructure.persistence.entity.RecurringTaskSeriesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RecurringTaskSeriesJpaRepository extends JpaRepository<RecurringTaskSeriesEntity, UUID> {
    List<RecurringTaskSeriesEntity> findBySpaceId(UUID spaceId);

    // A constructor expression straight onto the domain record: infrastructure may depend on
    // domain, and an identical projection record here would only add a mapping step. The point
    // of the shape is that nothing lands in the persistence context — see
    // RecurringTaskSeriesSchedule.
    @Query("""
        select new com.nido.api.tasks.domain.model.RecurringTaskSeriesSchedule(
            s.id, s.intervalType, s.intervalCount, s.leadIntervalType, s.leadIntervalCount,
            s.anchorDate, s.endDate, s.occurrenceCount)
        from RecurringTaskSeriesEntity s
        where s.spaceId = :spaceId
        """)
    List<RecurringTaskSeriesSchedule> findSchedulesBySpaceId(@Param("spaceId") UUID spaceId);

    // Held for the rest of the transaction: serializes concurrent materialization for
    // the same space so two requests can't both read the same occurrenceCount and each
    // insert the same occurrence — see RecurringTaskSeriesMaterializer.
    @Query(value = "select pg_advisory_xact_lock(hashtext(:key))", nativeQuery = true)
    void lockMaterialization(@Param("key") String key);
}
