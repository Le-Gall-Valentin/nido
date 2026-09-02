package com.nido.api.shopping.application.handler;

import com.nido.api.shopping.application.port.in.UpdateShoppingItemUseCase;
import com.nido.api.shopping.domain.model.ShoppingCategory;
import com.nido.api.shopping.domain.model.ShoppingException;
import com.nido.api.shopping.domain.model.ShoppingItem;
import com.nido.api.shopping.domain.model.UpdateShoppingItemCommand;
import com.nido.api.shopping.domain.port.out.ShoppingCategoryRepository;
import com.nido.api.shopping.domain.port.out.ShoppingItemRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class UpdateShoppingItemHandler implements UpdateShoppingItemUseCase {

    private final ShoppingItemRepository itemRepository;
    private final ShoppingCategoryRepository categoryRepository;

    public UpdateShoppingItemHandler(ShoppingItemRepository itemRepository, ShoppingCategoryRepository categoryRepository) {
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public ShoppingItem update(UpdateShoppingItemCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanWrite();
        ShoppingItem existing = itemRepository.findById(command.itemId()).orElseThrow(ShoppingException.ItemNotFound::new);
        if (!existing.spaceId().equals(command.spaceId())) {
            throw new ShoppingException.ItemNotFound();
        }
        ShoppingCategory category = categoryRepository.findById(command.categoryId())
            .orElseThrow(ShoppingException.CategoryNotFound::new);
        if (!category.spaceId().equals(command.spaceId())) {
            throw new ShoppingException.CategoryNotFound();
        }
        return itemRepository.update(command);
    }
}
