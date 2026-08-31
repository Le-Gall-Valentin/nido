package com.nido.api.shopping.application.port.in;

import com.nido.api.shopping.domain.model.RenameShoppingCategoryCommand;
import com.nido.api.shopping.domain.model.ShoppingCategory;
import com.nido.api.space.domain.model.SpaceMembership;

public interface RenameShoppingCategoryUseCase {
    ShoppingCategory rename(RenameShoppingCategoryCommand command, SpaceMembership caller);
}
