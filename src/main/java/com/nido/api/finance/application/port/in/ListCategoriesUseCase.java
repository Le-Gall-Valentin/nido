package com.nido.api.finance.application.port.in;

import com.nido.api.finance.domain.model.Category;
import com.nido.api.space.domain.model.SpaceMembership;

import java.util.List;

public interface ListCategoriesUseCase {
    List<Category> list(SpaceMembership caller);
}
