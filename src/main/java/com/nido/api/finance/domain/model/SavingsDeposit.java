package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A deposit on a savings goal, as a mail tells it.
 *
 * @param contributorName null for a member who can no longer be named
 * @param saved           what the goal holds once this deposit is in
 * @param savedByMember   what each member has put in, this deposit included
 */
public record SavingsDeposit(GoalFacts goal, UUID contributorId, String contributorName, BigDecimal amount, LocalDate date,
                             BigDecimal saved, List<MemberShare> savedByMember) {

    /** How far the goal is, in whole percent rounded down — a goal at 99.9 % is not reached. */
    public int percent() {
        return Percent.of(saved, goal.target());
    }

    /** Whether this deposit brings the goal to its target — a deposit can never take it past. */
    public boolean reachesGoal() {
        return saved.compareTo(goal.target()) >= 0;
    }
}
