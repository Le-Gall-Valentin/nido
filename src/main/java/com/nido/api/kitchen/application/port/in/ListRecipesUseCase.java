package com.nido.api.kitchen.application.port.in;

import com.nido.api.kitchen.domain.model.RecipeSummaryView;
import com.nido.api.space.domain.model.SpaceMembership;

import java.util.List;

public interface ListRecipesUseCase {
    List<RecipeSummaryView> list(SpaceMembership caller);
}
