package com.nido.api.shopping.application.port.in;

import com.nido.api.shopping.domain.model.ShoppingItem;
import com.nido.api.shopping.domain.model.UpdateShoppingItemCommand;
import com.nido.api.space.domain.model.SpaceMembership;

public interface UpdateShoppingItemUseCase {
    ShoppingItem update(UpdateShoppingItemCommand command, SpaceMembership caller);
}
