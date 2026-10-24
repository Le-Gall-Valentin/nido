package com.nido.api.finance.application.service;

import com.nido.api.finance.domain.model.GoalFacts;
import com.nido.api.finance.domain.model.MemberShare;
import com.nido.api.finance.domain.model.SavingsContribution;
import com.nido.api.finance.domain.model.SavingsDeposit;
import com.nido.api.finance.domain.model.SavingsGoal;
import com.nido.api.finance.domain.port.out.FinanceNotificationPort;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.Addressee;
import com.nido.api.space.domain.model.SpaceMembership;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Who hears about the savings of a space: every member but the author — a goal belongs to the whole space, and so
 * does what is put into it. A goal changed in nothing its members see (its colour, its icon) tells nobody.
 *
 * <p>Called by the handlers inside their transaction, after their change is written — before it, for a deletion,
 * which takes the deposits with it.
 */
@ApplicationService
public class SavingsNotifier {

    private static final Comparator<MemberShare> MOST_SAVED_FIRST = Comparator.comparing(MemberShare::amount).reversed()
        .thenComparing(MemberShare::memberName, Comparator.nullsLast(Comparator.naturalOrder()));

    private final FinanceAudience audience;
    private final FinanceNotificationPort notifications;

    public SavingsNotifier(FinanceAudience audience, FinanceNotificationPort notifications) {
        this.audience = audience;
        this.notifications = notifications;
    }

    /** @param contributionsAfter every deposit on the goal, this one included */
    public void deposited(SpaceMembership actor, SavingsGoal goal, SavingsContribution deposit,
                          List<SavingsContribution> contributionsAfter) {
        tellEveryone(actor, (heard, readers) -> notifications.savingsDeposited(heard.gesture(),
            new SavingsDeposit(GoalFacts.of(goal), deposit.memberId(), heard.nameOf(deposit.memberId()), deposit.amount(),
                deposit.date(), total(contributionsAfter), savedByMember(contributionsAfter, heard)),
            readers));
    }

    public void goalCreated(SpaceMembership actor, SavingsGoal goal) {
        tellEveryone(actor, (heard, readers) -> notifications.savingsGoalCreated(heard.gesture(), GoalFacts.of(goal), readers));
    }

    public void goalUpdated(SpaceMembership actor, SavingsGoal before, SavingsGoal after) {
        if (after.differsForMembersFrom(before)) {
            tellEveryone(actor, (heard, readers) -> notifications.savingsGoalUpdated(heard.gesture(),
                GoalFacts.of(before), GoalFacts.of(after), readers));
        }
    }

    /** @param contributions the deposits the goal held, read before it was deleted with them */
    public void goalDeleted(SpaceMembership actor, SavingsGoal goal, List<SavingsContribution> contributions) {
        tellEveryone(actor, (heard, readers) -> notifications.savingsGoalDeleted(heard.gesture(), GoalFacts.of(goal),
            total(contributions), savedByMember(contributions, heard), readers));
    }

    private void tellEveryone(SpaceMembership actor, BiConsumer<Audience, List<Addressee>> tell) {
        audience.of(actor).ifPresent(heard -> {
            List<Addressee> readers = heard.everyoneButTheActor();
            if (!readers.isEmpty()) {
                tell.accept(heard, readers);
            }
        });
    }

    private static BigDecimal total(List<SavingsContribution> contributions) {
        return contributions.stream().map(SavingsContribution::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** What each member has put in, the most first — a former member's deposits counted under no name. */
    private static List<MemberShare> savedByMember(List<SavingsContribution> contributions, Audience heard) {
        Map<UUID, BigDecimal> byMember = new LinkedHashMap<>();
        contributions.forEach(deposit -> byMember.merge(deposit.memberId(), deposit.amount(), BigDecimal::add));
        return byMember.entrySet().stream()
            .map(entry -> new MemberShare(entry.getKey(), heard.nameOf(entry.getKey()), entry.getValue()))
            .sorted(MOST_SAVED_FIRST)
            .toList();
    }
}
