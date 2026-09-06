package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.UpdateRecurringSeriesUseCase;
import com.nido.api.finance.application.service.SpaceMemberValidator;
import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.Contribution;
import com.nido.api.finance.domain.model.ContributionSplitter;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.RecurringTransactionSeries;
import com.nido.api.finance.domain.model.UpdateRecurringSeriesCommand;
import com.nido.api.finance.domain.port.out.CategoryRepository;
import com.nido.api.finance.domain.port.out.RecurringTransactionSeriesRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@ApplicationService
public class UpdateRecurringSeriesHandler implements UpdateRecurringSeriesUseCase {

    private final RecurringTransactionSeriesRepository seriesRepository;
    private final CategoryRepository categoryRepository;
    private final SpaceMemberValidator spaceMemberValidator;

    public UpdateRecurringSeriesHandler(
            RecurringTransactionSeriesRepository seriesRepository, CategoryRepository categoryRepository, SpaceMemberValidator spaceMemberValidator) {
        this.seriesRepository = seriesRepository;
        this.categoryRepository = categoryRepository;
        this.spaceMemberValidator = spaceMemberValidator;
    }

    @Override
    @Transactional
    public RecurringTransactionSeries update(UpdateRecurringSeriesCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanWrite();
        Category category = categoryRepository.findById(command.categoryId())
            .filter(c -> c.spaceId().equals(command.spaceId()))
            .orElseThrow(FinanceException.CategoryNotFound::new);
        if (category.type() != command.type()) {
            throw new FinanceException.CategoryTypeMismatch();
        }
        List<Contribution> resolved = ContributionSplitter.resolve(command.amount(), command.contributors());
        if (!resolved.isEmpty() && command.payerId() == null) {
            throw new FinanceException.PayerRequired();
        }
        if (command.endDate() != null && command.endDate().isBefore(command.anchorDate())) {
            throw new FinanceException.InvalidEndDate();
        }
        if (command.payerId() != null) {
            spaceMemberValidator.ensureMember(command.spaceId(), command.payerId());
        }
        resolved.forEach(c -> spaceMemberValidator.ensureMember(command.spaceId(), c.memberId()));
        RecurringTransactionSeries existing = seriesRepository.findById(command.seriesId())
            .orElseThrow(FinanceException.RecurringSeriesNotFound::new);
        if (!existing.spaceId().equals(command.spaceId())) {
            throw new FinanceException.RecurringSeriesNotFound();
        }
        return seriesRepository.update(command, resolved);
    }
}
