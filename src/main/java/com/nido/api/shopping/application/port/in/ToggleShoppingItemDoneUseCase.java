package com.nido.api.shopping.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;

import java.util.UUID;

public interface ToggleShoppingItemDoneUseCase {
    void toggle(UUID itemId, UUID spaceId, SpaceMembership caller);
}
