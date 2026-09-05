package com.nido.api.finance.application.port.in;

import com.nido.api.finance.domain.model.Budget;
import com.nido.api.space.domain.model.SpaceMembership;

import java.util.List;

public interface ListBudgetsUseCase {
    List<Budget> list(SpaceMembership caller);
}
