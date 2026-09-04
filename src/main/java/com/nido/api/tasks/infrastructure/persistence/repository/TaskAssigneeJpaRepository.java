package com.nido.api.tasks.infrastructure.persistence.repository;

import com.nido.api.tasks.infrastructure.persistence.entity.TaskAssigneeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TaskAssigneeJpaRepository extends JpaRepository<TaskAssigneeEntity, UUID> {
    List<TaskAssigneeEntity> findByTaskId(UUID taskId);
    List<TaskAssigneeEntity> findByTaskIdInOrderByTaskIdAsc(Collection<UUID> taskIds);
    void deleteByTaskId(UUID taskId);
}
