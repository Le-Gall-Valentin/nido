package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.GetOpeningBalanceUseCase;
import com.nido.api.finance.domain.model.OpeningBalance;
import com.nido.api.finance.domain.port.out.OpeningBalanceRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@ApplicationService
public class GetOpeningBalanceHandler implements GetOpeningBalanceUseCase {

    private final OpeningBalanceRepository openingBalances;

    public GetOpeningBalanceHandler(OpeningBalanceRepository openingBalances) {
        this.openingBalances = openingBalances;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OpeningBalance> get(SpaceMembership caller) {
        return openingBalances.findBySpaceId(caller.spaceId());
    }
}
