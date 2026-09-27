package com.nido.api.dashboard.infrastructure.source;

import com.nido.api.dashboard.domain.model.CardKind;
import com.nido.api.dashboard.domain.model.DashboardContext;
import com.nido.api.dashboard.domain.model.SavingsOutlook;
import com.nido.api.dashboard.domain.model.SourceResult;
import com.nido.api.dashboard.domain.port.out.DashboardSource;
import com.nido.api.finance.application.port.in.ListSavingsGoalsUseCase;
import com.nido.api.finance.domain.model.SavingsGoal;
import com.nido.api.finance.domain.model.SavingsGoalDetail;
import org.springframework.stereotype.Component;

import java.util.List;

/** Reads the space's savings goals from the finance module and lets {@link SavingsOutlook} decide the card. */
@Component
public class SavingsDashboardSource implements DashboardSource {

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
        if (!SavingsOutlook.hasSavings(context)) {
            // Not even asked for: there is nothing to show.
            return SourceResult.nothing();
        }
        List<SavingsOutlook.Goal> goals = listGoals.list(context.caller()).stream()
            .map(SavingsDashboardSource::toGoal)
            .toList();
        return SavingsOutlook.of(goals, context);
    }

    private static SavingsOutlook.Goal toGoal(SavingsGoalDetail detail) {
        SavingsGoal goal = detail.goal();
        return new SavingsOutlook.Goal(goal.id(), goal.name(), goal.glyph(), goal.color(), goal.targetAmount(),
            detail.totalContributed(), goal.targetDate());
    }
}
