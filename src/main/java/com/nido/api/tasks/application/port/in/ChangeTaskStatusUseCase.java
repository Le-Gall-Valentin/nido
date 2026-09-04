package com.nido.api.tasks.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.domain.model.Task;
import com.nido.api.tasks.domain.model.TaskStatus;

import java.util.UUID;

public interface ChangeTaskStatusUseCase {
    Task changeStatus(UUID taskId, UUID spaceId, TaskStatus newStatus, SpaceMembership caller);
}
