package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.SetOpeningBalanceUseCase;
import com.nido.api.finance.application.service.BalanceNotifier;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.OpeningBalance;
import com.nido.api.finance.domain.model.SetOpeningBalanceCommand;
import com.nido.api.finance.domain.port.out.OpeningBalanceRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.GetSpaceTodayUseCase;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@ApplicationService
public class SetOpeningBalanceHandler implements SetOpeningBalanceUseCase {

    private final OpeningBalanceRepository openingBalances;
    private final GetSpaceTodayUseCase spaceToday;
    private final BalanceNotifier notifier;

    public SetOpeningBalanceHandler(OpeningBalanceRepository openingBalances, GetSpaceTodayUseCase spaceToday,
                                    BalanceNotifier notifier) {
        this.openingBalances = openingBalances;
        this.spaceToday = spaceToday;
        this.notifier = notifier;
    }

    @Override
    @Transactional
    public OpeningBalance set(SetOpeningBalanceCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanWrite();
        if (command.asOf().isAfter(spaceToday.today(command.spaceId()))) {
            throw new FinanceException.OpeningBalanceInFuture();
        }
        Optional<OpeningBalance> before = openingBalances.findBySpaceId(command.spaceId());
        OpeningBalance saved = openingBalances.save(new OpeningBalance(command.spaceId(), command.amount(), command.asOf()));
        notifier.openingBalanceSet(caller, before, saved);
        return saved;
    }
}
