package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.SetOpeningBalanceUseCase;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.OpeningBalance;
import com.nido.api.finance.domain.model.SetOpeningBalanceCommand;
import com.nido.api.finance.domain.port.out.OpeningBalanceRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.GetSpaceTodayUseCase;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class SetOpeningBalanceHandler implements SetOpeningBalanceUseCase {

    private final OpeningBalanceRepository openingBalances;
    private final GetSpaceTodayUseCase spaceToday;

    public SetOpeningBalanceHandler(OpeningBalanceRepository openingBalances, GetSpaceTodayUseCase spaceToday) {
        this.openingBalances = openingBalances;
        this.spaceToday = spaceToday;
    }

    @Override
    @Transactional
    public OpeningBalance set(SetOpeningBalanceCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanWrite();
        if (command.asOf().isAfter(spaceToday.today(command.spaceId()))) {
            throw new FinanceException.OpeningBalanceInFuture();
        }
        return openingBalances.save(new OpeningBalance(command.spaceId(), command.amount(), command.asOf()));
    }
}
