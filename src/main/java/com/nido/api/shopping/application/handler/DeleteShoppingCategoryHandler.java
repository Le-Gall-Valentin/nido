package com.nido.api.shopping.application.handler;

import com.nido.api.shopping.application.port.in.DeleteShoppingCategoryUseCase;
import com.nido.api.shopping.domain.model.ShoppingCategory;
import com.nido.api.shopping.domain.model.ShoppingException;
import com.nido.api.shopping.domain.port.out.ShoppingCategoryRepository;
import com.nido.api.shopping.domain.port.out.ShoppingItemRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class DeleteShoppingCategoryHandler implements DeleteShoppingCategoryUseCase {

    private final ShoppingCategoryRepository categoryRepository;
    private final ShoppingItemRepository itemRepository;

    public DeleteShoppingCategoryHandler(ShoppingCategoryRepository categoryRepository, ShoppingItemRepository itemRepository) {
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
    }

    @Override
    @Transactional
    public void delete(UUID categoryId, UUID spaceId, SpaceMembership caller) {
        caller.ensureSameSpace(spaceId);
        caller.ensureCanWrite();
        ShoppingCategory existing = categoryRepository.findById(categoryId).orElseThrow(ShoppingException.CategoryNotFound::new);
        if (!existing.spaceId().equals(spaceId)) {
            throw new ShoppingException.CategoryNotFound();
        }
        if (existing.fallback()) {
            throw new ShoppingException.CannotDeleteFallbackCategory();
        }
        ShoppingCategory fallback = categoryRepository.findBySpaceId(spaceId).stream()
            .filter(ShoppingCategory::fallback).findFirst()
            .orElseThrow(ShoppingException.CategoryNotFound::new);
        itemRepository.reassignCategory(categoryId, fallback.id());
        categoryRepository.delete(categoryId);
    }
}
