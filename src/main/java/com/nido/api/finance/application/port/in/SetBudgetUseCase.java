package com.nido.api.finance.application.port.in;

import com.nido.api.finance.domain.model.Budget;
import com.nido.api.finance.domain.model.SetBudgetCommand;
import com.nido.api.space.domain.model.SpaceMembership;

public interface SetBudgetUseCase {
    Budget set(SetBudgetCommand command, SpaceMembership caller);
}
