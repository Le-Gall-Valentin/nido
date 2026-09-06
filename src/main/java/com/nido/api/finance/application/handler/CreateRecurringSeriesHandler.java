package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.CreateRecurringSeriesUseCase;
import com.nido.api.finance.domain.model.Contribution;
import com.nido.api.finance.domain.model.ContributionSplitter;
import com.nido.api.finance.domain.model.CreateRecurringSeriesCommand;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.RecurringTransactionSeries;
import com.nido.api.finance.domain.port.out.CategoryRepository;
import com.nido.api.finance.domain.port.out.RecurringTransactionSeriesRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@ApplicationService
public class CreateRecurringSeriesHandler implements CreateRecurringSeriesUseCase {

    private final RecurringTransactionSeriesRepository seriesRepository;
    private final CategoryRepository categoryRepository;

    public CreateRecurringSeriesHandler(RecurringTransactionSeriesRepository seriesRepository, CategoryRepository categoryRepository) {
        this.seriesRepository = seriesRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public RecurringTransactionSeries create(CreateRecurringSeriesCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanWrite();
        categoryRepository.findById(command.categoryId())
            .filter(category -> category.spaceId().equals(command.spaceId()))
            .orElseThrow(FinanceException.CategoryNotFound::new);
        List<Contribution> resolved = ContributionSplitter.resolve(command.amount(), command.contributors());
        if (!resolved.isEmpty() && command.payerId() == null) {
            throw new FinanceException.PayerRequired();
        }
        if (command.endDate() != null && command.endDate().isBefore(command.anchorDate())) {
            throw new FinanceException.InvalidEndDate();
        }
        return seriesRepository.create(command, resolved);
    }
}
