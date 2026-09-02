package com.nido.api.shopping.application.handler;

import com.nido.api.shopping.application.port.in.CreateShoppingCategoryUseCase;
import com.nido.api.shopping.domain.model.ShoppingCategory;
import com.nido.api.shopping.domain.port.out.ShoppingCategoryRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class CreateShoppingCategoryHandler implements CreateShoppingCategoryUseCase {

    private final ShoppingCategoryRepository categoryRepository;

    public CreateShoppingCategoryHandler(ShoppingCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public ShoppingCategory create(UUID spaceId, String name, SpaceMembership caller) {
        caller.ensureSameSpace(spaceId);
        caller.ensureCanWrite();
        // A space that has never had its categories listed yet still needs its
        // fallback category to exist before a custom one can safely be deleted later.
        DefaultShoppingCategorySeeder.seedIfMissing(categoryRepository, spaceId);
        return categoryRepository.create(spaceId, name, false);
    }
}
