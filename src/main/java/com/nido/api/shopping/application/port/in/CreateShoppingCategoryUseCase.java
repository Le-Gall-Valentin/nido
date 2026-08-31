package com.nido.api.shopping.application.port.in;

import com.nido.api.shopping.domain.model.ShoppingCategory;
import com.nido.api.space.domain.model.SpaceMembership;

import java.util.UUID;

public interface CreateShoppingCategoryUseCase {
    ShoppingCategory create(UUID spaceId, String name, SpaceMembership caller);
}
