package com.nido.api.shopping.application.handler;

import com.nido.api.shopping.application.port.in.ListShoppingItemsUseCase;
import com.nido.api.shopping.domain.model.ShoppingItem;
import com.nido.api.shopping.domain.port.out.ShoppingItemRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;

import java.util.List;

@ApplicationService
public class ListShoppingItemsHandler implements ListShoppingItemsUseCase {

    private final ShoppingItemRepository itemRepository;

    public ListShoppingItemsHandler(ShoppingItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Override
    public List<ShoppingItem> list(SpaceMembership caller) {
        return itemRepository.findBySpaceId(caller.spaceId());
    }
}
