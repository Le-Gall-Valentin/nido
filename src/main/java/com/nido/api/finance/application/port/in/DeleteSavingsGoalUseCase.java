package com.nido.api.finance.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;

import java.util.UUID;

public interface DeleteSavingsGoalUseCase {
    void delete(UUID goalId, UUID spaceId, SpaceMembership caller);
}
