package com.nido.api.finance.application.port.in;

import com.nido.api.finance.domain.model.OpeningBalance;
import com.nido.api.finance.domain.model.SetOpeningBalanceCommand;
import com.nido.api.space.domain.model.SpaceMembership;

public interface SetOpeningBalanceUseCase {
    OpeningBalance set(SetOpeningBalanceCommand command, SpaceMembership caller);
}
