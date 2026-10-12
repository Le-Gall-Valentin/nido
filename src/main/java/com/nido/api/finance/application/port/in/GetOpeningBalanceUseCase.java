package com.nido.api.finance.application.port.in;

import com.nido.api.finance.domain.model.OpeningBalance;
import com.nido.api.space.domain.model.SpaceMembership;

import java.util.Optional;

public interface GetOpeningBalanceUseCase {
    Optional<OpeningBalance> get(SpaceMembership caller);
}
