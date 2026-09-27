package com.nido.api.dashboard.infrastructure.source;

import com.nido.api.dashboard.domain.model.AttentionItem;
import com.nido.api.dashboard.domain.model.BalanceDirection;
import com.nido.api.dashboard.domain.model.BalanceWithMember;
import com.nido.api.dashboard.domain.model.BudgetWatch;
import com.nido.api.dashboard.domain.model.CardKind;
import com.nido.api.dashboard.domain.model.DashboardContext;
import com.nido.api.dashboard.domain.model.FinanceCard;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The month in figures, the budgets worth watching, the recurring operations of the coming week and,
 * in a shared space, who owes whom from the caller's side. Always present.
 */
@Component
public class FinanceDashboardSource implements DashboardSource {

    static final int UPCOMING_CAP = 5;

    private static final Comparator<UpcomingOperation> UPCOMING_ORDER =
        Comparator.comparing(UpcomingOperation::date).thenComparing(UpcomingOperation::label);
    private static final Comparator<BalanceWithMember> BALANCE_ORDER =
        Comparator.comparing(BalanceWithMember::direction)
            .thenComparing(BalanceWithMember::amount, Comparator.reverseOrder());

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

        List<BudgetLine> watched = stats.budgetVsActual().stream()
            .filter(line -> line.status() != BudgetStatus.OK)
            .filter(line -> categoryById.containsKey(line.categoryId()))
            .sorted(Comparator.comparingDouble(FinanceDashboardSource::ratio).reversed())
            .toList();
        List<BudgetWatch> budgetsToWatch = watched.stream()
            .map(line -> {
                Category category = categoryById.get(line.categoryId());
                return new BudgetWatch(line.categoryId(), category.label(), category.color(),
                    line.spent(), line.monthlyLimit(), statusOf(line.status()));
            })
            .toList();

        List<AttentionItem> attention = new ArrayList<>();
        watched.stream()
            .filter(line -> line.status() == BudgetStatus.OVER)
            .forEach(line -> attention.add(new AttentionItem.BudgetOverrun(line.categoryId(),
                categoryById.get(line.categoryId()).label(), line.spent(), line.monthlyLimit())));

        List<BalanceWithMember> balances = null;
        if (context.isShared()) {
            balances = balancesOf(context);
            balances.stream()
                .filter(balance -> balance.direction() == BalanceDirection.I_OWE)
                .forEach(balance -> attention.add(new AttentionItem.Debt(balance.memberId(), balance.amount())));
        }

        return SourceResult.of(new FinanceCard(month, stats.balance(), stats.totalExpense(), stats.totalIncome(),
            stats.remainingBudget(), budgetsToWatch, upcoming(caller, today.plusDays(1), today.plusDays(7)), balances),
            attention);
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
        return Stream.concat(created, projected).sorted(UPCOMING_ORDER).limit(UPCOMING_CAP).toList();
    }

    private List<BalanceWithMember> balancesOf(DashboardContext context) {
        List<BalanceWithMember> balances = new ArrayList<>();
        for (SuggestedTransfer transfer : getBalances.getBalances(context.caller()).suggestedTransfers()) {
            if (transfer.fromMemberId().equals(context.callerId())) {
                balances.add(new BalanceWithMember(transfer.toMemberId(), transfer.amount(), BalanceDirection.I_OWE));
            } else if (transfer.toMemberId().equals(context.callerId())) {
                balances.add(new BalanceWithMember(transfer.fromMemberId(), transfer.amount(), BalanceDirection.OWES_ME));
            }
        }
        balances.sort(BALANCE_ORDER);
        return List.copyOf(balances);
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

    /** spent ÷ limit. A 0 € budget ("spend nothing here") with spending outranks every other line. */
    private static double ratio(BudgetLine line) {
        if (line.monthlyLimit().signum() == 0) {
            return line.spent().signum() > 0 ? Double.POSITIVE_INFINITY : 0;
        }
        return line.spent().doubleValue() / line.monthlyLimit().doubleValue();
    }
}
