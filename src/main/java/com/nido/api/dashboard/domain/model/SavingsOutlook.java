package com.nido.api.dashboard.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * The savings card: the household's goals, most pressing first. A goal has no start date, so there is
 * no "pace" to judge; what the card says instead is how much is left to put aside each month.
 */
public final class SavingsOutlook {

    public static final int GOAL_CAP = 4;
    /** A target at most this many days away is due soon. */
    public static final int DUE_SOON_DAYS = 30;

    /** A goal as the finance module keeps it, with everything put toward it so far. */
    public record Goal(UUID goalId, String name, String glyph, String color, BigDecimal target,
                       BigDecimal contributed, LocalDate targetDate) {
    }

    private static final Comparator<SavingsGoalItem> MOST_PRESSING_FIRST =
        Comparator.comparing(SavingsGoalItem::state)
            .thenComparing(SavingsGoalItem::targetDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(SavingsGoalItem::name);

    private SavingsOutlook() {}

    /** Shared goals, in a shared space only — as on the finance page; a personal space shows none. */
    public static boolean hasSavings(DashboardContext context) {
        return context.isShared();
    }

    public static SourceResult of(List<Goal> goals, DashboardContext context) {
        if (!hasSavings(context) || goals.isEmpty()) {
            return SourceResult.nothing();
        }
        return SourceResult.of(new SavingsCard(goals.stream()
            .map(goal -> item(goal, context.today()))
            .sorted(MOST_PRESSING_FIRST)
            .limit(GOAL_CAP)
            .toList()));
    }

    private static SavingsGoalItem item(Goal goal, LocalDate today) {
        GoalState state = state(goal, today);
        BigDecimal monthlyNeeded = state == GoalState.REACHED || goal.targetDate() == null
            ? null
            : monthlyNeeded(goal.target().subtract(goal.contributed()), goal.targetDate(), today);
        return new SavingsGoalItem(goal.goalId(), goal.name(), goal.glyph(), goal.color(), goal.target(),
            goal.contributed(), goal.targetDate(), state, monthlyNeeded);
    }

    private static GoalState state(Goal goal, LocalDate today) {
        if (goal.contributed().compareTo(goal.target()) >= 0) {
            return GoalState.REACHED;
        }
        if (goal.targetDate() == null) {
            return GoalState.IN_PROGRESS;
        }
        if (goal.targetDate().isBefore(today)) {
            return GoalState.PAST_DUE;
        }
        if (!goal.targetDate().isAfter(today.plusDays(DUE_SOON_DAYS))) {
            return GoalState.DUE_SOON;
        }
        return GoalState.IN_PROGRESS;
    }

    /** At least one month: a target this month, or already past, asks for the whole remainder now. */
    private static BigDecimal monthlyNeeded(BigDecimal remaining, LocalDate targetDate, LocalDate today) {
        long months = Math.max(1, ChronoUnit.MONTHS.between(YearMonth.from(today), YearMonth.from(targetDate)));
        return remaining.divide(BigDecimal.valueOf(months), 2, RoundingMode.CEILING);
    }
}
