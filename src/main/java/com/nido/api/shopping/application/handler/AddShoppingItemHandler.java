package com.nido.api.shopping.application.handler;

import com.nido.api.shopping.application.port.in.AddShoppingItemUseCase;
import com.nido.api.shopping.domain.model.AddShoppingItemCommand;
import com.nido.api.shopping.domain.model.ShoppingCategory;
import com.nido.api.shopping.domain.model.ShoppingException;
import com.nido.api.shopping.domain.model.ShoppingItem;
import com.nido.api.shopping.domain.port.out.ShoppingCategoryRepository;
import com.nido.api.shopping.domain.port.out.ShoppingItemRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class AddShoppingItemHandler implements AddShoppingItemUseCase {

    private final ShoppingItemRepository itemRepository;
    private final ShoppingCategoryRepository categoryRepository;

    public AddShoppingItemHandler(ShoppingItemRepository itemRepository, ShoppingCategoryRepository categoryRepository) {
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public ShoppingItem add(AddShoppingItemCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanWrite();
        ShoppingCategory category = categoryRepository.findById(command.categoryId())
            .orElseThrow(ShoppingException.CategoryNotFound::new);
        if (!category.spaceId().equals(command.spaceId())) {
            throw new ShoppingException.CategoryNotFound();
        }
        return itemRepository.add(command);
    }
}
