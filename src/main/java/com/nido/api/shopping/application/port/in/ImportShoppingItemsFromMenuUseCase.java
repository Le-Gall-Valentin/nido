package com.nido.api.shopping.application.port.in;

import com.nido.api.shopping.domain.model.ImportShoppingItemsCommand;
import com.nido.api.shopping.domain.model.ShoppingItem;
import com.nido.api.space.domain.model.SpaceMembership;

import java.util.List;

public interface ImportShoppingItemsFromMenuUseCase {
    List<ShoppingItem> importItems(ImportShoppingItemsCommand command, SpaceMembership caller);
}
