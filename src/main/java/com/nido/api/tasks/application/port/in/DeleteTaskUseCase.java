package com.nido.api.tasks.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;

import java.util.UUID;

public interface DeleteTaskUseCase {
    void delete(UUID taskId, UUID spaceId, SpaceMembership caller);
}
