package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.MaterializeDueRecurringTransactionsUseCase;
import com.nido.api.finance.domain.port.out.RecurringTransactionSeriesRepository;
import com.nido.api.finance.domain.port.out.TransactionRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.GetSpaceTodayUseCase;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@ApplicationService
public class MaterializeDueRecurringTransactionsHandler implements MaterializeDueRecurringTransactionsUseCase {

    private final TransactionRepository transactionRepository;
    private final RecurringTransactionSeriesRepository seriesRepository;
    private final GetSpaceTodayUseCase spaceToday;

    public MaterializeDueRecurringTransactionsHandler(TransactionRepository transactionRepository,
                                                      RecurringTransactionSeriesRepository seriesRepository,
                                                      GetSpaceTodayUseCase spaceToday) {
        this.transactionRepository = transactionRepository;
        this.seriesRepository = seriesRepository;
        this.spaceToday = spaceToday;
    }

    @Override
    @Transactional
    public void materialize(SpaceMembership caller) {
        materialize(caller, spaceToday.today(caller.spaceId()));
    }

    /**
     * Package-visible overload with an explicit "today" — lets tests materialize deterministically.
     * Annotated in its own right so a caller reaching it through the Spring proxy still gets the
     * transaction boundary the advisory lock in {@code RecurringTransactionMaterializer} depends on.
     */
    @Transactional
    void materialize(SpaceMembership caller, LocalDate today) {
        RecurringTransactionMaterializer.materializeDueOccurrences(transactionRepository, seriesRepository, caller.spaceId(), today);
    }
}
