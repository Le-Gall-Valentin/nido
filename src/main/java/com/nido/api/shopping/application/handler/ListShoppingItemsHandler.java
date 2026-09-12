package com.nido.api.shopping.application.handler;

import com.nido.api.shopping.application.port.in.ListShoppingItemsUseCase;
import com.nido.api.shopping.domain.model.ShoppingItem;
import com.nido.api.shopping.domain.port.out.ShoppingItemRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@ApplicationService
public class ListShoppingItemsHandler implements ListShoppingItemsUseCase {

    private final ShoppingItemRepository itemRepository;

    public ListShoppingItemsHandler(ShoppingItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShoppingItem> list(SpaceMembership caller) {
        return itemRepository.findBySpaceId(caller.spaceId());
    }
}
