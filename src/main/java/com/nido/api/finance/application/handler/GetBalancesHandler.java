package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.GetBalancesUseCase;
import com.nido.api.finance.domain.model.BalanceCalculator;
import com.nido.api.finance.domain.model.Balances;
import com.nido.api.finance.domain.port.out.RunningTotalsRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class GetBalancesHandler implements GetBalancesUseCase {

    private final RunningTotalsRepository runningTotals;

    public GetBalancesHandler(RunningTotalsRepository runningTotals) {
        this.runningTotals = runningTotals;
    }

    /** Not read-only: see RunningTotalsRepository, whose first read of a space builds its totals. */
    @Override
    @Transactional
    public Balances getBalances(SpaceMembership caller) {
        return BalanceCalculator.fromNets(runningTotals.findNetByMember(caller.spaceId()));
    }
}
