package com.nido.api.tasks.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;

import java.util.UUID;

public interface ToggleSubtaskUseCase {
    void toggle(UUID taskId, UUID subtaskId, UUID spaceId, SpaceMembership caller);
}
