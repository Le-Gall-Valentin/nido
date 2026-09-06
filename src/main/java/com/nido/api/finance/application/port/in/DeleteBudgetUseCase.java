package com.nido.api.finance.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;

import java.util.UUID;

public interface DeleteBudgetUseCase {
    void delete(UUID spaceId, UUID categoryId, SpaceMembership caller);
}
