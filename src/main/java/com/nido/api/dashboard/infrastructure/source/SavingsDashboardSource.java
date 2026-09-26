package com.nido.api.dashboard.infrastructure.source;

import com.nido.api.dashboard.domain.model.CardKind;
import com.nido.api.dashboard.domain.model.DashboardContext;
import com.nido.api.dashboard.domain.model.GoalState;
import com.nido.api.dashboard.domain.model.SavingsCard;
import com.nido.api.dashboard.domain.model.SavingsGoalItem;
import com.nido.api.dashboard.domain.model.SourceResult;
import com.nido.api.dashboard.domain.port.out.DashboardSource;
import com.nido.api.finance.application.port.in.ListSavingsGoalsUseCase;
import com.nido.api.finance.domain.model.SavingsGoal;
import com.nido.api.finance.domain.model.SavingsGoalDetail;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/**
 * The household's savings goals, most pressing first. A goal has no start date, so there is no
 * "pace" to judge; what the card says instead is how much is left to put aside each month.
 */
@Component
public class SavingsDashboardSource implements DashboardSource {

    static final int GOAL_CAP = 4;
    static final int DUE_SOON_DAYS = 30;

    private static final Comparator<SavingsGoalItem> ORDER =
        Comparator.comparing(SavingsGoalItem::state)
            .thenComparing(SavingsGoalItem::targetDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(SavingsGoalItem::name);

    private final ListSavingsGoalsUseCase listGoals;

    public SavingsDashboardSource(ListSavingsGoalsUseCase listGoals) {
        this.listGoals = listGoals;
    }

    @Override
    public CardKind kind() {
        return CardKind.SAVINGS;
    }

    @Override
    public SourceResult read(DashboardContext context) {
        if (!context.isShared()) {
            return SourceResult.nothing();
        }
        List<SavingsGoalDetail> goals = listGoals.list(context.caller());
        if (goals.isEmpty()) {
            return SourceResult.nothing();
        }
        return SourceResult.of(new SavingsCard(goals.stream()
            .map(detail -> toItem(detail, context.today()))
            .sorted(ORDER)
            .limit(GOAL_CAP)
            .toList()));
    }

    private static SavingsGoalItem toItem(SavingsGoalDetail detail, LocalDate today) {
        SavingsGoal goal = detail.goal();
        BigDecimal contributed = detail.totalContributed();
        GoalState state = state(goal.targetAmount(), contributed, goal.targetDate(), today);
        BigDecimal monthlyNeeded = state == GoalState.REACHED || goal.targetDate() == null
            ? null
            : monthlyNeeded(goal.targetAmount().subtract(contributed), goal.targetDate(), today);
        return new SavingsGoalItem(goal.id(), goal.name(), goal.glyph(), goal.color(), goal.targetAmount(),
            contributed, goal.targetDate(), state, monthlyNeeded);
    }

    private static GoalState state(BigDecimal target, BigDecimal contributed, LocalDate targetDate, LocalDate today) {
        if (contributed.compareTo(target) >= 0) {
            return GoalState.REACHED;
        }
        if (targetDate == null) {
            return GoalState.IN_PROGRESS;
        }
        if (targetDate.isBefore(today)) {
            return GoalState.PAST_DUE;
        }
        if (!targetDate.isAfter(today.plusDays(DUE_SOON_DAYS))) {
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
