package com.nido.api.finance.application.port.in;

import com.nido.api.finance.domain.model.SavingsGoal;
import com.nido.api.finance.domain.model.UpdateSavingsGoalCommand;
import com.nido.api.space.domain.model.SpaceMembership;

public interface UpdateSavingsGoalUseCase {
    SavingsGoal update(UpdateSavingsGoalCommand command, SpaceMembership caller);
}
