package com.nido.api.finance.application.port.in;

import com.nido.api.finance.domain.model.SavingsGoalDetail;
import com.nido.api.finance.domain.model.UpdateSavingsGoalCommand;
import com.nido.api.space.domain.model.SpaceMembership;

public interface UpdateSavingsGoalUseCase {
    SavingsGoalDetail update(UpdateSavingsGoalCommand command, SpaceMembership caller);
}
