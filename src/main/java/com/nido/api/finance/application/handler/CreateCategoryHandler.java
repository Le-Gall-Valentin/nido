package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.CreateCategoryUseCase;
import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.CreateCategoryCommand;
import com.nido.api.finance.domain.port.out.CategoryRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class CreateCategoryHandler implements CreateCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public CreateCategoryHandler(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public Category create(CreateCategoryCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanWrite();
        DefaultFinanceCategorySeeder.seedIfMissing(categoryRepository, command.spaceId());
        return categoryRepository.create(command, false);
    }
}
