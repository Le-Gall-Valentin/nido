package com.nido.api.finance.application.port.in;

import com.nido.api.finance.domain.model.Balances;
import com.nido.api.space.domain.model.SpaceMembership;

public interface GetBalancesUseCase {
    Balances getBalances(SpaceMembership caller);
}
