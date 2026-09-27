package com.nido.api.dashboard.domain.model;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * The finance card and its "À traiter" items: the month in figures, the budgets worth a look — most
 * consumed first —, the recurring operations of the coming week, soonest first, and, in a shared space,
 * who owes whom from the caller's side. Always a card.
 */
public final class FinanceReview {

    public static final int UPCOMING_CAP = 5;

    /** The month's figures, as the finance module computes them. */
    public record Figures(YearMonth month, BigDecimal balance, BigDecimal totalExpense, BigDecimal totalIncome,
                          BigDecimal remainingBudget) {
    }

    /** A transfer the finance module suggests to settle the household's balances. */
    public record Transfer(UUID fromMemberId, UUID toMemberId, BigDecimal amount) {
    }

    private static final Comparator<BudgetWatch> MOST_CONSUMED_FIRST =
        Comparator.comparingDouble(FinanceReview::consumed).reversed();
    private static final Comparator<UpcomingOperation> SOONEST_FIRST =
        Comparator.comparing(UpcomingOperation::date).thenComparing(UpcomingOperation::label);
    /** What the caller owes before what they are owed, the largest amount first. */
    private static final Comparator<BalanceWithMember> BALANCE_ORDER =
        Comparator.comparing(BalanceWithMember::direction)
            .thenComparing(BalanceWithMember::amount, Comparator.reverseOrder());

    private FinanceReview() {}

    /** Balances between members exist only in a shared space: in a personal one there is nobody to owe. */
    public static boolean hasBalances(DashboardContext context) {
        return context.isShared();
    }

    /**
     * @param transfers the suggested transfers; unused outside a shared space, where they need not even be read
     */
    public static SourceResult of(Figures figures, List<BudgetWatch> budgets, List<UpcomingOperation> upcoming,
                                  List<Transfer> transfers, DashboardContext context) {
        List<BudgetWatch> budgetsToWatch = budgets.stream().sorted(MOST_CONSUMED_FIRST).toList();

        List<AttentionItem> attention = new ArrayList<>();
        budgetsToWatch.stream()
            .filter(budget -> budget.status() == BudgetWatch.Status.OVER)
            .forEach(budget -> attention.add(
                new AttentionItem.BudgetOverrun(budget.categoryId(), budget.label(), budget.spent(), budget.limit())));

        List<BalanceWithMember> balances = null;
        if (hasBalances(context)) {
            balances = balancesSeenBy(context.callerId(), transfers);
            balances.stream()
                .filter(balance -> balance.direction() == BalanceDirection.I_OWE)
                .forEach(balance -> attention.add(new AttentionItem.Debt(balance.memberId(), balance.amount())));
        }

        List<UpcomingOperation> soonest = upcoming.stream().sorted(SOONEST_FIRST).limit(UPCOMING_CAP).toList();

        return SourceResult.of(new FinanceCard(figures.month(), figures.balance(), figures.totalExpense(),
            figures.totalIncome(), figures.remainingBudget(), budgetsToWatch, soonest, balances), attention);
    }

    /** Only the transfers the caller is part of: the others are between other members. */
    private static List<BalanceWithMember> balancesSeenBy(UUID callerId, List<Transfer> transfers) {
        List<BalanceWithMember> balances = new ArrayList<>();
        for (Transfer transfer : transfers) {
            if (transfer.fromMemberId().equals(callerId)) {
                balances.add(new BalanceWithMember(transfer.toMemberId(), transfer.amount(), BalanceDirection.I_OWE));
            } else if (transfer.toMemberId().equals(callerId)) {
                balances.add(new BalanceWithMember(transfer.fromMemberId(), transfer.amount(), BalanceDirection.OWES_ME));
            }
        }
        balances.sort(BALANCE_ORDER);
        return balances;
    }

    /** spent ÷ limit. A 0 € budget ("spend nothing here") with spending outranks every other line. */
    private static double consumed(BudgetWatch budget) {
        if (budget.limit().signum() == 0) {
            return budget.spent().signum() > 0 ? Double.POSITIVE_INFINITY : 0;
        }
        return budget.spent().doubleValue() / budget.limit().doubleValue();
    }
}
