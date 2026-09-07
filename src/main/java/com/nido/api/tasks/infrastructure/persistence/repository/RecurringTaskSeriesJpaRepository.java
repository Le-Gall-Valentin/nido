package com.nido.api.tasks.infrastructure.persistence.repository;

import com.nido.api.tasks.infrastructure.persistence.entity.RecurringTaskSeriesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RecurringTaskSeriesJpaRepository extends JpaRepository<RecurringTaskSeriesEntity, UUID> {
    List<RecurringTaskSeriesEntity> findBySpaceId(UUID spaceId);

    // Held for the rest of the transaction: serializes concurrent materialization for
    // the same space so two requests can't both read the same occurrenceCount and each
    // insert the same occurrence — see RecurringTaskSeriesMaterializer.
    @Query(value = "select pg_advisory_xact_lock(hashtext(:key))", nativeQuery = true)
    void lockMaterialization(@Param("key") String key);
}
