package com.nido.api.tasks.infrastructure.persistence.repository;

import com.nido.api.tasks.infrastructure.persistence.entity.RecurringTaskSeriesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RecurringTaskSeriesJpaRepository extends JpaRepository<RecurringTaskSeriesEntity, UUID> {
}
