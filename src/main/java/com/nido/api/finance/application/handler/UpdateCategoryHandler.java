package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.UpdateCategoryUseCase;
import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.UpdateCategoryCommand;
import com.nido.api.finance.domain.port.out.CategoryRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class UpdateCategoryHandler implements UpdateCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public UpdateCategoryHandler(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public Category update(UpdateCategoryCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanWrite();
        return categoryRepository.update(command);
    }
}
