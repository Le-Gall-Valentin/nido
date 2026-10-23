package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.ClearOpeningBalanceUseCase;
import com.nido.api.finance.application.service.BalanceNotifier;
import com.nido.api.finance.domain.model.OpeningBalance;
import com.nido.api.finance.domain.port.out.OpeningBalanceRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@ApplicationService
public class ClearOpeningBalanceHandler implements ClearOpeningBalanceUseCase {

    private final OpeningBalanceRepository openingBalances;
    private final BalanceNotifier notifier;

    public ClearOpeningBalanceHandler(OpeningBalanceRepository openingBalances, BalanceNotifier notifier) {
        this.openingBalances = openingBalances;
        this.notifier = notifier;
    }

    @Override
    @Transactional
    public void clear(UUID spaceId, SpaceMembership caller) {
        caller.ensureSameSpace(spaceId);
        caller.ensureCanWrite();
        // Read first: whether there was one decides whether anyone is told it is gone.
        Optional<OpeningBalance> before = openingBalances.findBySpaceId(spaceId);
        openingBalances.deleteBySpaceId(spaceId);
        notifier.openingBalanceCleared(caller, before);
    }
}
