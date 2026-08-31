package com.nido.api.shopping.application.port.in;

import com.nido.api.shopping.domain.model.AddShoppingItemCommand;
import com.nido.api.shopping.domain.model.ShoppingItem;
import com.nido.api.space.domain.model.SpaceMembership;

public interface AddShoppingItemUseCase {
    ShoppingItem add(AddShoppingItemCommand command, SpaceMembership caller);
}
