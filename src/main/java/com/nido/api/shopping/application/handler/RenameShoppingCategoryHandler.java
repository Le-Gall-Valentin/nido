package com.nido.api.shopping.application.handler;

import com.nido.api.shopping.application.port.in.RenameShoppingCategoryUseCase;
import com.nido.api.shopping.domain.model.RenameShoppingCategoryCommand;
import com.nido.api.shopping.domain.model.ShoppingCategory;
import com.nido.api.shopping.domain.model.ShoppingException;
import com.nido.api.shopping.domain.port.out.ShoppingCategoryRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class RenameShoppingCategoryHandler implements RenameShoppingCategoryUseCase {

    private final ShoppingCategoryRepository categoryRepository;

    public RenameShoppingCategoryHandler(ShoppingCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public ShoppingCategory rename(RenameShoppingCategoryCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanWrite();
        ShoppingCategory existing = categoryRepository.findById(command.categoryId())
            .orElseThrow(ShoppingException.CategoryNotFound::new);
        if (!existing.spaceId().equals(command.spaceId())) {
            throw new ShoppingException.CategoryNotFound();
        }
        return categoryRepository.rename(command.categoryId(), command.name());
    }
}
