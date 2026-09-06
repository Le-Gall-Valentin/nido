package com.nido.api.finance.application.port.in;

import com.nido.api.finance.domain.model.CreateSavingsGoalCommand;
import com.nido.api.finance.domain.model.SavingsGoal;
import com.nido.api.space.domain.model.SpaceMembership;

public interface CreateSavingsGoalUseCase {
    SavingsGoal create(CreateSavingsGoalCommand command, SpaceMembership caller);
}
