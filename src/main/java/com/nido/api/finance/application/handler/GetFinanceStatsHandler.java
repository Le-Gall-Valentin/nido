package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.GetFinanceStatsUseCase;
import com.nido.api.finance.application.service.RunningBalanceReader;
import com.nido.api.finance.domain.model.RunningBalance;
import com.nido.api.finance.domain.model.Budget;
import com.nido.api.finance.domain.model.BudgetLine;
import com.nido.api.finance.domain.model.CategoryAmount;
import com.nido.api.finance.domain.model.FinanceStats;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.finance.domain.port.out.BudgetRepository;
import com.nido.api.finance.domain.port.out.RecurringTransactionSeriesRepository;
import com.nido.api.finance.domain.port.out.TransactionRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.application.port.in.GetSpaceTodayUseCase;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationService
public class GetFinanceStatsHandler implements GetFinanceStatsUseCase {

    private final GetSpaceTodayUseCase spaceToday;
    private final TransactionRepository transactionRepository;
    private final RecurringTransactionSeriesRepository seriesRepository;
    private final BudgetRepository budgetRepository;
    private final RunningBalanceReader runningBalanceReader;

    public GetFinanceStatsHandler(
            TransactionRepository transactionRepository, RecurringTransactionSeriesRepository seriesRepository,
            BudgetRepository budgetRepository, RunningBalanceReader runningBalanceReader,
                            GetSpaceTodayUseCase spaceToday) {
        this.transactionRepository = transactionRepository;
        this.seriesRepository = seriesRepository;
        this.budgetRepository = budgetRepository;
        this.runningBalanceReader = runningBalanceReader;
        this.spaceToday = spaceToday;
    }

    @Override
    @Transactional
    public FinanceStats getStats(YearMonth month, SpaceMembership caller) {
        return getStats(month, caller, spaceToday.today(caller.spaceId()));
    }

    /**
     * Package-visible overload with an explicit "today" — lets tests materialize deterministically.
     * Annotated in its own right (not just via the public overload) so an external caller invoking
     * it directly through the Spring proxy — e.g. an integration test — still gets the transaction
     * boundary the advisory lock in {@code RecurringTransactionMaterializer} depends on.
     */
    @Transactional
    FinanceStats getStats(YearMonth month, SpaceMembership caller, LocalDate today) {
        RecurringTransactionMaterializer.materializeDueOccurrences(transactionRepository, seriesRepository, caller.spaceId(), today);
        List<Transaction> transactions = transactionRepository.findBySpaceIdAndMonth(caller.spaceId(), month);

        BigDecimal totalExpense = sum(transactions, TransactionType.EXPENSE);
        BigDecimal totalIncome = sum(transactions, TransactionType.INCOME);
        BigDecimal monthNet = totalIncome.subtract(totalExpense);

        // The current month shows today's balance, as a bank does: an operation entered for later in the month counts in
        // the month's figures, not yet in the balance. Any other month shows the balance at its end.
        RunningBalance running = runningBalanceReader.read(caller.spaceId());
        boolean current = month.equals(YearMonth.from(today));
        LocalDate balanceAsOf = current ? today : month.atEndOfMonth();
        BigDecimal balance = current
            ? running.at(today, signedSum(transactions.stream().filter(t -> !t.date().isAfter(today)).toList()))
            : running.atEndOf(month);

        Map<UUID, BigDecimal> spentByCategory = transactions.stream()
            .filter(t -> t.type() == TransactionType.EXPENSE)
            .collect(Collectors.groupingBy(Transaction::categoryId, Collectors.reducing(BigDecimal.ZERO, Transaction::amount, BigDecimal::add)));
        // Every category, not just expense ones — the frontend now splits this by the
        // category's own (fixed) type for its two breakdown views (spending vs income).
        Map<UUID, BigDecimal> amountByCategory = transactions.stream()
            .collect(Collectors.groupingBy(Transaction::categoryId, Collectors.reducing(BigDecimal.ZERO, Transaction::amount, BigDecimal::add)));
        List<CategoryAmount> breakdown = amountByCategory.entrySet().stream()
            .map(e -> new CategoryAmount(e.getKey(), e.getValue())).toList();

        List<Budget> budgets = budgetRepository.findBySpaceId(caller.spaceId());
        List<BudgetLine> budgetVsActual = budgets.stream()
            .map(b -> new BudgetLine(b.categoryId(), b.monthlyLimit(), spentByCategory.getOrDefault(b.categoryId(), BigDecimal.ZERO)))
            .toList();
        // Spending in a category left deliberately unbudgeted has no cap, so it must not eat
        // into this figure — only sum what's left on categories that actually have a budget.
        BigDecimal remainingBudget = budgetVsActual.stream()
            .map(line -> line.monthlyLimit().subtract(line.spent()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new FinanceStats(balance, balanceAsOf, monthNet, totalExpense, totalIncome, remainingBudget, breakdown, budgetVsActual);
    }

    private static BigDecimal signedSum(List<Transaction> transactions) {
        return sum(transactions, TransactionType.INCOME).subtract(sum(transactions, TransactionType.EXPENSE));
    }

    private static BigDecimal sum(List<Transaction> transactions, TransactionType type) {
        return transactions.stream().filter(t -> t.type() == type).map(Transaction::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
