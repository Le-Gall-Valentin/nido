package com.nido.api.tasks.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.domain.model.Task;

import java.util.UUID;

public interface MoveTaskUseCase {
    Task move(UUID taskId, UUID destinationSpaceId, SpaceMembership caller);
}
