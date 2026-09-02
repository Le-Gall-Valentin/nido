package com.nido.api.shopping.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;

import java.util.UUID;

public interface DeleteShoppingCategoryUseCase {
    void delete(UUID categoryId, UUID spaceId, SpaceMembership caller);
}
