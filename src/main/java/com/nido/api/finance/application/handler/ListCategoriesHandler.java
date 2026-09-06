package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.ListCategoriesUseCase;
import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.port.out.CategoryRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@ApplicationService
public class ListCategoriesHandler implements ListCategoriesUseCase {

    private final CategoryRepository categoryRepository;

    public ListCategoriesHandler(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public List<Category> list(SpaceMembership caller) {
        DefaultFinanceCategorySeeder.seedIfMissing(categoryRepository, caller.spaceId());
        return categoryRepository.findBySpaceId(caller.spaceId());
    }
}
