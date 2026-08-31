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

    private static final List<String> DEFAULT_CATEGORY_NAMES = List.of(
        "Fruits & légumes", "Viande & poisson", "Crémerie", "Épicerie", "Surgelés", "Boissons", "Hygiène & entretien");
    private static final String DEFAULT_FALLBACK_CATEGORY_NAME = "Maison & divers";

    private final ShoppingCategoryRepository categoryRepository;

    public ListShoppingCategoriesHandler(ShoppingCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public List<ShoppingCategory> list(SpaceMembership caller) {
        if (!categoryRepository.existsBySpaceId(caller.spaceId())) {
            DEFAULT_CATEGORY_NAMES.forEach(name -> categoryRepository.create(caller.spaceId(), name, false));
            categoryRepository.create(caller.spaceId(), DEFAULT_FALLBACK_CATEGORY_NAME, true);
        }
        return categoryRepository.findBySpaceId(caller.spaceId());
    }
}
