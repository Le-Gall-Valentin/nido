package com.nido.api.finance.application.port.in;

import com.nido.api.finance.domain.model.SavingsGoalDetail;
import com.nido.api.space.domain.model.SpaceMembership;

import java.util.List;

public interface ListSavingsGoalsUseCase {
    List<SavingsGoalDetail> list(SpaceMembership caller);
}
