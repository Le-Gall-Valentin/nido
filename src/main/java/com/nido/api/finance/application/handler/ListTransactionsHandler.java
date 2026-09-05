package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.ListTransactionsUseCase;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.port.out.RecurringTransactionSeriesRepository;
import com.nido.api.finance.domain.port.out.TransactionRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@ApplicationService
public class ListTransactionsHandler implements ListTransactionsUseCase {

    private final TransactionRepository transactionRepository;
    private final RecurringTransactionSeriesRepository seriesRepository;

    public ListTransactionsHandler(TransactionRepository transactionRepository, RecurringTransactionSeriesRepository seriesRepository) {
        this.transactionRepository = transactionRepository;
        this.seriesRepository = seriesRepository;
    }

    @Override
    @Transactional
    public List<Transaction> list(YearMonth month, SpaceMembership caller) {
        return list(month, caller, LocalDate.now());
    }

    /**
     * Package-visible overload with an explicit "today" — lets tests materialize deterministically.
     * Annotated in its own right (not just via the public overload) so an external caller invoking
     * it directly through the Spring proxy — e.g. an integration test — still gets the transaction
     * boundary the advisory lock in {@code RecurringTransactionMaterializer} depends on.
     */
    @Transactional
    List<Transaction> list(YearMonth month, SpaceMembership caller, LocalDate today) {
        RecurringTransactionMaterializer.materializeDueOccurrences(transactionRepository, seriesRepository, caller.spaceId(), today);
        return transactionRepository.findBySpaceIdAndMonth(caller.spaceId(), month);
    }
}
