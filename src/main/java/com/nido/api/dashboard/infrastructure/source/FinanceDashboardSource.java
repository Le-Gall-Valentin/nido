package com.nido.api.dashboard.infrastructure.source;

import com.nido.api.dashboard.domain.model.BudgetWatch;
import com.nido.api.dashboard.domain.model.CardKind;
import com.nido.api.dashboard.domain.model.DashboardContext;
import com.nido.api.dashboard.domain.model.FinanceReview;
import com.nido.api.dashboard.domain.model.SourceResult;
import com.nido.api.dashboard.domain.model.UpcomingOperation;
import com.nido.api.dashboard.domain.port.out.DashboardSource;
import com.nido.api.finance.application.port.in.GetBalancesUseCase;
import com.nido.api.finance.application.port.in.GetFinanceStatsUseCase;
import com.nido.api.finance.application.port.in.ListCategoriesUseCase;
import com.nido.api.finance.application.port.in.ListTransactionsInRangeUseCase;
import com.nido.api.finance.application.port.in.ProjectRecurringSeriesUseCase;
import com.nido.api.finance.domain.model.BudgetLine;
import com.nido.api.finance.domain.model.BudgetStatus;
import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.FinanceStats;
import com.nido.api.finance.domain.model.SuggestedTransfer;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Reads the month's figures, the budgets, the recurring operations of the coming week and, in a shared
 * space, the suggested transfers from the finance module, and lets {@link FinanceReview} decide the card.
 */
@Component
public class FinanceDashboardSource implements DashboardSource {

    private final GetFinanceStatsUseCase getStats;
    private final ListCategoriesUseCase listCategories;
    private final ListTransactionsInRangeUseCase listTransactions;
    private final ProjectRecurringSeriesUseCase projectSeries;
    private final GetBalancesUseCase getBalances;

    public FinanceDashboardSource(GetFinanceStatsUseCase getStats, ListCategoriesUseCase listCategories,
                                  ListTransactionsInRangeUseCase listTransactions,
                                  ProjectRecurringSeriesUseCase projectSeries, GetBalancesUseCase getBalances) {
        this.getStats = getStats;
        this.listCategories = listCategories;
        this.listTransactions = listTransactions;
        this.projectSeries = projectSeries;
        this.getBalances = getBalances;
    }

    @Override
    public CardKind kind() {
        return CardKind.FINANCE;
    }

    @Override
    public SourceResult read(DashboardContext context) {
        SpaceMembership caller = context.caller();
        LocalDate today = context.today();
        YearMonth month = YearMonth.from(today);
        FinanceStats stats = getStats.getStats(month, caller);
        Map<UUID, Category> categoryById = listCategories.list(caller).stream()
            .collect(Collectors.toMap(Category::id, Function.identity()));

        FinanceReview.Figures figures = new FinanceReview.Figures(month, stats.balance(), stats.totalExpense(),
            stats.totalIncome(), stats.remainingBudget());
        // A line within its budget has no BudgetWatch, and one whose category is gone has no name to show.
        List<BudgetWatch> budgets = stats.budgetVsActual().stream()
            .filter(line -> line.status() != BudgetStatus.OK)
            .filter(line -> categoryById.containsKey(line.categoryId()))
            .map(line -> toWatch(line, categoryById.get(line.categoryId())))
            .toList();
        List<FinanceReview.Transfer> transfers = FinanceReview.hasBalances(context)
            ? getBalances.getBalances(caller).suggestedTransfers().stream().map(FinanceDashboardSource::toTransfer).toList()
            : List.of();

        return FinanceReview.of(figures, budgets, upcoming(caller, today.plusDays(1), today.plusDays(7)), transfers, context);
    }

    /**
     * Recurring operations only: the ones a series already produced in the window, plus the ones it
     * will. The projection starts after the last materialized date, so no date is counted twice.
     */
    private List<UpcomingOperation> upcoming(SpaceMembership caller, LocalDate from, LocalDate to) {
        Stream<UpcomingOperation> created = listTransactions.list(caller, from, to).stream()
            .filter(transaction -> transaction.recurringSeriesId() != null)
            .map(transaction -> new UpcomingOperation(transaction.date(), transaction.label(), transaction.amount(),
                typeOf(transaction.type()), transaction.recurringSeriesId()));
        Stream<UpcomingOperation> projected = projectSeries.project(caller, from, to).stream()
            .map(occurrence -> new UpcomingOperation(occurrence.date(), occurrence.label(), occurrence.amount(),
                typeOf(occurrence.type()), occurrence.seriesId()));
        return Stream.concat(created, projected).toList();
    }

    private static BudgetWatch toWatch(BudgetLine line, Category category) {
        return new BudgetWatch(line.categoryId(), category.label(), category.color(), line.spent(), line.monthlyLimit(),
            statusOf(line.status()));
    }

    private static FinanceReview.Transfer toTransfer(SuggestedTransfer transfer) {
        return new FinanceReview.Transfer(transfer.fromMemberId(), transfer.toMemberId(), transfer.amount());
    }

    /** Only a line worth watching is ever converted: one within its budget never reaches the card. */
    static BudgetWatch.Status statusOf(BudgetStatus status) {
        return switch (status) {
            case WARNING -> BudgetWatch.Status.WARNING;
            case OVER -> BudgetWatch.Status.OVER;
            case OK -> throw new IllegalArgumentException("A budget within its limit is not watched");
        };
    }

    static UpcomingOperation.Type typeOf(TransactionType type) {
        return switch (type) {
            case EXPENSE -> UpcomingOperation.Type.EXPENSE;
            case INCOME -> UpcomingOperation.Type.INCOME;
        };
    }
}
