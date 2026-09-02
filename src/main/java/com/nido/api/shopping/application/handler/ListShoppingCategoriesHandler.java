package com.nido.api.shopping.application.handler;

import com.nido.api.shopping.application.port.in.ListShoppingCategoriesUseCase;
import com.nido.api.shopping.domain.model.ShoppingCategory;
import com.nido.api.shopping.domain.port.out.ShoppingCategoryRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@ApplicationService
public class ListShoppingCategoriesHandler implements ListShoppingCategoriesUseCase {

    private final ShoppingCategoryRepository categoryRepository;

    public ListShoppingCategoriesHandler(ShoppingCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public List<ShoppingCategory> list(SpaceMembership caller) {
        DefaultShoppingCategorySeeder.seedIfMissing(categoryRepository, caller.spaceId());
        return categoryRepository.findBySpaceId(caller.spaceId());
    }
}
