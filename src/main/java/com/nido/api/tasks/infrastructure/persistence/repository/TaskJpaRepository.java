package com.nido.api.tasks.infrastructure.persistence.repository;

import com.nido.api.tasks.infrastructure.persistence.entity.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TaskJpaRepository extends JpaRepository<TaskEntity, UUID> {
    List<TaskEntity> findBySpaceId(UUID spaceId);

    List<TaskEntity> findBySpaceIdAndDueDateBetween(UUID spaceId, LocalDate from, LocalDate to);
}
