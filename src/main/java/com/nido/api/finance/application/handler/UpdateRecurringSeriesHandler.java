package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.UpdateRecurringSeriesUseCase;
import com.nido.api.finance.domain.model.Contribution;
import com.nido.api.finance.domain.model.ContributionSplitter;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.RecurringTransactionSeries;
import com.nido.api.finance.domain.model.UpdateRecurringSeriesCommand;
import com.nido.api.finance.domain.port.out.RecurringTransactionSeriesRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@ApplicationService
public class UpdateRecurringSeriesHandler implements UpdateRecurringSeriesUseCase {

    private final RecurringTransactionSeriesRepository seriesRepository;

    public UpdateRecurringSeriesHandler(RecurringTransactionSeriesRepository seriesRepository) {
        this.seriesRepository = seriesRepository;
    }

    @Override
    @Transactional
    public RecurringTransactionSeries update(UpdateRecurringSeriesCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanWrite();
        List<Contribution> resolved = ContributionSplitter.resolve(command.amount(), command.contributors());
        if (!resolved.isEmpty() && command.payerId() == null) {
            throw new FinanceException.PayerRequired();
        }
        RecurringTransactionSeries existing = seriesRepository.findById(command.seriesId())
            .orElseThrow(FinanceException.RecurringSeriesNotFound::new);
        if (!existing.spaceId().equals(command.spaceId())) {
            throw new FinanceException.RecurringSeriesNotFound();
        }
        return seriesRepository.update(command, resolved);
    }
}
