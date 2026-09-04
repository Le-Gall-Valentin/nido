package com.nido.api.tasks.infrastructure.persistence.repository;

import com.nido.api.tasks.infrastructure.persistence.entity.RecurringTaskSeriesSubtaskTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecurringTaskSeriesSubtaskTemplateJpaRepository extends JpaRepository<RecurringTaskSeriesSubtaskTemplateEntity, UUID> {
    List<RecurringTaskSeriesSubtaskTemplateEntity> findBySeriesIdOrderByPositionAsc(UUID seriesId);
    void deleteBySeriesId(UUID seriesId);
}
