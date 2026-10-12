package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.ClearOpeningBalanceUseCase;
import com.nido.api.finance.domain.port.out.OpeningBalanceRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class ClearOpeningBalanceHandler implements ClearOpeningBalanceUseCase {

    private final OpeningBalanceRepository openingBalances;

    public ClearOpeningBalanceHandler(OpeningBalanceRepository openingBalances) {
        this.openingBalances = openingBalances;
    }

    @Override
    @Transactional
    public void clear(UUID spaceId, SpaceMembership caller) {
        caller.ensureSameSpace(spaceId);
        caller.ensureCanWrite();
        openingBalances.deleteBySpaceId(spaceId);
    }
}
