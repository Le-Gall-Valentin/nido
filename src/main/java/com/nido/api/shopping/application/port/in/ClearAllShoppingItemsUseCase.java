package com.nido.api.shopping.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;

import java.util.UUID;

public interface ClearAllShoppingItemsUseCase {
    void clearAll(UUID spaceId, SpaceMembership caller);
}
