package com.nido.api.finance.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;

import java.util.UUID;

public interface DeleteCategoryUseCase {
    void delete(UUID categoryId, UUID spaceId, SpaceMembership caller);
}
