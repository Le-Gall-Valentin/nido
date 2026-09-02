package com.nido.api.shopping.application.handler;

import com.nido.api.shopping.application.port.in.DeleteShoppingItemUseCase;
import com.nido.api.shopping.domain.model.ShoppingException;
import com.nido.api.shopping.domain.model.ShoppingItem;
import com.nido.api.shopping.domain.port.out.ShoppingItemRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class DeleteShoppingItemHandler implements DeleteShoppingItemUseCase {

    private final ShoppingItemRepository itemRepository;

    public DeleteShoppingItemHandler(ShoppingItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Override
    @Transactional
    public void delete(UUID itemId, UUID spaceId, SpaceMembership caller) {
        caller.ensureSameSpace(spaceId);
        caller.ensureCanWrite();
        ShoppingItem existing = itemRepository.findById(itemId).orElseThrow(ShoppingException.ItemNotFound::new);
        if (!existing.spaceId().equals(spaceId)) {
            throw new ShoppingException.ItemNotFound();
        }
        itemRepository.delete(itemId);
    }
}
