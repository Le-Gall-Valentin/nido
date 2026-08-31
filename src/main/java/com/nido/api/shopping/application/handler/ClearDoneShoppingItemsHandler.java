package com.nido.api.shopping.application.handler;

import com.nido.api.shopping.application.port.in.ClearDoneShoppingItemsUseCase;
import com.nido.api.shopping.domain.port.out.ShoppingItemRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class ClearDoneShoppingItemsHandler implements ClearDoneShoppingItemsUseCase {

    private final ShoppingItemRepository itemRepository;

    public ClearDoneShoppingItemsHandler(ShoppingItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Override
    @Transactional
    public void clearDone(UUID spaceId, SpaceMembership caller) {
        caller.ensureSameSpace(spaceId);
        caller.ensureCanWrite();
        itemRepository.deleteDoneBySpaceId(spaceId);
    }
}
