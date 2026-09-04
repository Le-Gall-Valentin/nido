package com.nido.api.tasks.infrastructure.persistence.repository;

import com.nido.api.tasks.infrastructure.persistence.entity.TaskSubtaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TaskSubtaskJpaRepository extends JpaRepository<TaskSubtaskEntity, UUID> {
    List<TaskSubtaskEntity> findByTaskIdOrderByPositionAsc(UUID taskId);
    List<TaskSubtaskEntity> findByTaskIdInOrderByTaskIdAscPositionAsc(Collection<UUID> taskIds);
    void deleteByTaskId(UUID taskId);
}
