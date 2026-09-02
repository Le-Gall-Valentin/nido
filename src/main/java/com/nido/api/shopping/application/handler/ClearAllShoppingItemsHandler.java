package com.nido.api.shopping.application.handler;

import com.nido.api.shopping.application.port.in.ClearAllShoppingItemsUseCase;
import com.nido.api.shopping.domain.port.out.ShoppingItemRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class ClearAllShoppingItemsHandler implements ClearAllShoppingItemsUseCase {

    private final ShoppingItemRepository itemRepository;

    public ClearAllShoppingItemsHandler(ShoppingItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Override
    @Transactional
    public void clearAll(UUID spaceId, SpaceMembership caller) {
        caller.ensureSameSpace(spaceId);
        caller.ensureCanWrite();
        itemRepository.deleteAllBySpaceId(spaceId);
    }
}
