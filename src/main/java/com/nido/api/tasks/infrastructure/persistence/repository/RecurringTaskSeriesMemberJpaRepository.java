package com.nido.api.tasks.infrastructure.persistence.repository;

import com.nido.api.tasks.infrastructure.persistence.entity.RecurringTaskSeriesMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecurringTaskSeriesMemberJpaRepository extends JpaRepository<RecurringTaskSeriesMemberEntity, UUID> {
    List<RecurringTaskSeriesMemberEntity> findBySeriesIdOrderByPositionAsc(UUID seriesId);
    void deleteBySeriesId(UUID seriesId);
}
