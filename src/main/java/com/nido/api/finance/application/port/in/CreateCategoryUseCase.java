package com.nido.api.finance.application.port.in;

import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.CreateCategoryCommand;
import com.nido.api.space.domain.model.SpaceMembership;

public interface CreateCategoryUseCase {
    Category create(CreateCategoryCommand command, SpaceMembership caller);
}
