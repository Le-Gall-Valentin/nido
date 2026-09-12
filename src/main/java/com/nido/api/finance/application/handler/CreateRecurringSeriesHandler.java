package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.CreateRecurringSeriesUseCase;
import com.nido.api.finance.application.service.SpaceMemberValidator;
import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.Contribution;
import com.nido.api.finance.domain.model.ContributionSplitter;
import com.nido.api.finance.domain.model.CreateRecurringSeriesCommand;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.RecurrenceProjector;
import com.nido.api.finance.domain.model.RecurringTransactionSeries;
import com.nido.api.finance.domain.port.out.CategoryRepository;
import com.nido.api.finance.domain.port.out.RecurringTransactionSeriesRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.application.port.in.GetSpaceTodayUseCase;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@ApplicationService
public class CreateRecurringSeriesHandler implements CreateRecurringSeriesUseCase {

    private final GetSpaceTodayUseCase spaceToday;
    private final RecurringTransactionSeriesRepository seriesRepository;
    private final CategoryRepository categoryRepository;
    private final SpaceMemberValidator spaceMemberValidator;

    public CreateRecurringSeriesHandler(
            RecurringTransactionSeriesRepository seriesRepository, CategoryRepository categoryRepository, SpaceMemberValidator spaceMemberValidator,
                            GetSpaceTodayUseCase spaceToday) {
        this.seriesRepository = seriesRepository;
        this.categoryRepository = categoryRepository;
        this.spaceMemberValidator = spaceMemberValidator;
        this.spaceToday = spaceToday;
    }

    @Override
    @Transactional
    public RecurringTransactionSeries create(CreateRecurringSeriesCommand command, SpaceMembership caller) {
        return create(command, caller, spaceToday.today(caller.spaceId()));
    }

    /**
     * Package-visible overload with an explicit "today" — lets tests exercise the backlog
     * ceiling against fixed dates instead of whenever the suite happens to run.
     */
    @Transactional
    RecurringTransactionSeries create(CreateRecurringSeriesCommand command, SpaceMembership caller, LocalDate today) {
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
        // A brand-new series has materialized nothing yet, so its whole past is backlog.
        RecurrenceProjector.validateBacklog(command.anchorDate(), command.intervalType(),
            command.intervalCount(), command.endDate(), today, null);
        if (command.payerId() != null) {
            spaceMemberValidator.ensureMember(command.spaceId(), command.payerId());
        }
        resolved.forEach(c -> spaceMemberValidator.ensureMember(command.spaceId(), c.memberId()));
        return seriesRepository.create(command, resolved);
    }
}
