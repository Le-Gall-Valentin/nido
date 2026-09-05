package com.nido.api.finance.application.port.in;

import com.nido.api.finance.domain.model.AddSavingsContributionCommand;
import com.nido.api.finance.domain.model.SavingsContribution;
import com.nido.api.space.domain.model.SpaceMembership;

public interface AddSavingsContributionUseCase {
    SavingsContribution add(AddSavingsContributionCommand command, SpaceMembership caller);
}
