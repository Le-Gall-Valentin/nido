package com.nido.api.finance.application.service;

import com.nido.api.finance.domain.model.Gesture;
import com.nido.api.finance.domain.model.GoalFacts;
import com.nido.api.finance.domain.model.MemberShare;
import com.nido.api.finance.domain.model.SavingsContribution;
import com.nido.api.finance.domain.model.SavingsDeposit;
import com.nido.api.finance.domain.model.SavingsGoal;
import com.nido.api.finance.domain.port.out.FinanceNotificationPort;
import com.nido.api.space.application.port.in.GetSpaceUseCase;
import com.nido.api.space.application.port.in.ListSpaceMembersUseCase;
import com.nido.api.space.domain.model.Addressee;
import com.nido.api.space.domain.model.SpaceDetailView;
import com.nido.api.space.domain.model.SpaceMemberView;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Who hears about the savings of a space: every member but the author — a goal is the whole space's. */
@ExtendWith(MockitoExtension.class)
class SavingsNotifierTest {

    @Mock ListSpaceMembersUseCase members;
    @Mock GetSpaceUseCase spaces;
    @Mock FinanceNotificationPort port;

    private final UUID spaceId = UUID.randomUUID();
    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();
    private final UUID carol = UUID.randomUUID();
    private final SpaceMembership aliceActs = new SpaceMembership(UUID.randomUUID(), spaceId, alice, SpaceRole.MEMBER, Instant.now());
    private final Gesture gesture = new Gesture(spaceId, "Chez nous", alice, "alice");
    private final SavingsGoal holidays = goal("1000.00", "#c17a5c");

    private SavingsNotifier notifier;

    @BeforeEach
    void setUp() {
        lenient().when(members.list(aliceActs)).thenReturn(List.of(member(alice, "alice"), member(bob, "bob"), member(carol, "carol")));
        lenient().when(spaces.get(spaceId, aliceActs)).thenReturn(new SpaceDetailView(spaceId, SpaceType.SHARED, "Chez nous",
            null, "#c17a5c", "🏡", ZoneId.of("Europe/Paris"), SpaceRole.MEMBER, 3));
        notifier = new SavingsNotifier(new FinanceAudience(members, spaces), port);
    }

    private static SpaceMemberView member(UUID id, String name) {
        return new SpaceMemberView(id, name, name + "@test.local", SpaceRole.MEMBER, Instant.now());
    }

    private SavingsGoal goal(String target, String color) {
        return new SavingsGoal(UUID.fromString("00000000-0000-0000-0000-000000000007"), spaceId, "Vacances",
            new BigDecimal(target), null, color, "🏖️");
    }

    private SavingsContribution deposit(UUID member, String amount) {
        return new SavingsContribution(UUID.randomUUID(), holidays.id(), member, new BigDecimal(amount), LocalDate.of(2026, 10, 21));
    }

    @Test
    void a_deposit_tells_every_member_but_its_author_with_the_total_saved_after_it() {
        SavingsContribution earlier = deposit(bob, "400.00");
        SavingsContribution now = deposit(alice, "100.00");

        notifier.deposited(aliceActs, holidays, now, List.of(earlier, now));

        verify(port).savingsDeposited(gesture, new SavingsDeposit(GoalFacts.of(holidays), alice, "alice",
            new BigDecimal("100.00"), LocalDate.of(2026, 10, 21), new BigDecimal("500.00"),
            List.of(new MemberShare(bob, "bob", new BigDecimal("400.00")), new MemberShare(alice, "alice", new BigDecimal("100.00")))),
            List.of(new Addressee(bob, "bob"), new Addressee(carol, "carol")));
    }

    @Test
    void a_deposit_in_another_member_s_name_tells_that_member_too() {
        SavingsContribution forBob = deposit(bob, "100.00");

        notifier.deposited(aliceActs, holidays, forBob, List.of(forBob));

        verify(port).savingsDeposited(gesture, new SavingsDeposit(GoalFacts.of(holidays), bob, "bob",
            new BigDecimal("100.00"), LocalDate.of(2026, 10, 21), new BigDecimal("100.00"),
            List.of(new MemberShare(bob, "bob", new BigDecimal("100.00")))),
            List.of(new Addressee(bob, "bob"), new Addressee(carol, "carol")));
    }

    @Test
    void a_new_goal_tells_everyone_but_its_author() {
        notifier.goalCreated(aliceActs, holidays);

        verify(port).savingsGoalCreated(gesture, GoalFacts.of(holidays), List.of(new Addressee(bob, "bob"), new Addressee(carol, "carol")));
    }

    @Test
    void a_goal_change_of_colour_only_tells_nobody() {
        notifier.goalUpdated(aliceActs, holidays, goal("1000.00", "#5c7a58"));

        verifyNoInteractions(port, members, spaces);
    }

    @Test
    void a_goal_change_of_target_tells_everyone_but_the_author() {
        SavingsGoal raised = goal("1200.00", "#c17a5c");

        notifier.goalUpdated(aliceActs, holidays, raised);

        verify(port).savingsGoalUpdated(gesture, GoalFacts.of(holidays), GoalFacts.of(raised),
            List.of(new Addressee(bob, "bob"), new Addressee(carol, "carol")));
    }

    @Test
    void a_deleted_goal_says_who_had_saved_what() {
        notifier.goalDeleted(aliceActs, holidays, List.of(deposit(bob, "200.00"), deposit(alice, "300.00"), deposit(bob, "100.00")));

        verify(port).savingsGoalDeleted(gesture, GoalFacts.of(holidays), new BigDecimal("600.00"),
            List.of(new MemberShare(alice, "alice", new BigDecimal("300.00")), new MemberShare(bob, "bob", new BigDecimal("300.00"))),
            List.of(new Addressee(bob, "bob"), new Addressee(carol, "carol")));
    }

    @Test
    void what_a_former_member_saved_is_still_counted_under_no_name() {
        UUID formerMember = UUID.randomUUID();

        notifier.goalDeleted(aliceActs, holidays, List.of(deposit(formerMember, "250.00"), deposit(bob, "100.00")));

        verify(port).savingsGoalDeleted(gesture, GoalFacts.of(holidays), new BigDecimal("350.00"),
            List.of(new MemberShare(formerMember, null, new BigDecimal("250.00")), new MemberShare(bob, "bob", new BigDecimal("100.00"))),
            List.of(new Addressee(bob, "bob"), new Addressee(carol, "carol")));
    }

    @Test
    void a_goal_in_a_personal_space_tells_nobody() {
        when(members.list(aliceActs)).thenReturn(List.of(member(alice, "alice")));

        notifier.goalCreated(aliceActs, holidays);

        verifyNoInteractions(port);
    }
}
