package com.nido.api.tasks.domain.port.out;

import com.nido.api.tasks.domain.model.CreateTaskCommand;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskStatus;
import com.nido.api.tasks.domain.model.UpdateTaskCommand;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository {
    Optional<Task> findById(UUID taskId);
    List<Task> findBySpaceId(UUID spaceId);
    Task create(CreateTaskCommand command);
    Task update(UpdateTaskCommand command);
    Task updateStatus(UUID taskId, TaskStatus status);
    Task toggleSubtask(UUID taskId, UUID subtaskId);
    void delete(UUID taskId);
}
