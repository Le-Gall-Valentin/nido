package com.nido.api.finance.infrastructure.notification;

import com.nido.api.finance.domain.model.BudgetAlert;
import com.nido.api.finance.domain.model.BudgetStatus;
import com.nido.api.finance.domain.model.ContributorSide;
import com.nido.api.finance.domain.model.Gesture;
import com.nido.api.finance.domain.model.GoalFacts;
import com.nido.api.finance.domain.model.Involved;
import com.nido.api.finance.domain.model.Involvement;
import com.nido.api.finance.domain.model.MemberShare;
import com.nido.api.finance.domain.model.OperationFacts;
import com.nido.api.finance.domain.model.RecurrenceInterval;
import com.nido.api.finance.domain.model.SavingsDeposit;
import com.nido.api.finance.domain.model.SeriesFacts;
import com.nido.api.finance.domain.model.SettlementAuthor;
import com.nido.api.finance.domain.model.Standing;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.notifications.application.port.in.NotifyUseCase;
import com.nido.api.notifications.domain.model.NotificationRequest;
import com.nido.api.space.domain.model.Addressee;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** One request per reader, each greeting them by name and pointing at the finances of the space. */
@ExtendWith(MockitoExtension.class)
class FinanceNotificationAdapterTest {

    @Mock NotifyUseCase notify;

    private final UUID spaceId = UUID.fromString("00000000-0000-0000-0000-000000000042");
    private final AppPath finance = new AppPath("/s/00000000-0000-0000-0000-000000000042/finance");
    private final UUID aliceId = UUID.randomUUID();
    private final Gesture gesture = new Gesture(spaceId, "Chez nous", aliceId, "alice");
    private final Addressee bob = new Addressee(UUID.randomUUID(), "bob");
    private final Addressee carol = new Addressee(UUID.randomUUID(), "carol");
    private final OperationFacts groceries = new OperationFacts("Courses", new BigDecimal("86.40"), TransactionType.EXPENSE,
        "Alimentation", LocalDate.of(2026, 10, 21), aliceId, "alice",
        List.of(new MemberShare(bob.userId(), "bob", new BigDecimal("43.20"))));
    private final SeriesFacts rent = new SeriesFacts("Loyer", new BigDecimal("1200.00"), TransactionType.EXPENSE, "Logement",
        aliceId, "alice", List.of(), RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 11, 1), null);

    private FinanceNotificationAdapter adapter() {
        return new FinanceNotificationAdapter(notify);
    }

    private List<NotificationRequest> sent(int count) {
        ArgumentCaptor<NotificationRequest> requests = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notify, times(count)).notify(requests.capture());
        return requests.getAllValues();
    }

    @Test
    void one_request_per_reader_greeting_them_by_name() {
        adapter().transactionAdded(gesture, groceries, List.of(bob, carol));

        List<NotificationRequest> requests = sent(2);
        assertThat(requests).extracting(NotificationRequest::recipientId).containsExactly(bob.userId(), carol.userId());
        assertThat(requests).extracting(NotificationRequest::expiresAt).containsOnlyNulls();
        assertThat(requests).extracting(NotificationRequest::fallback).containsOnlyNulls();
        assertThat(requests.getFirst().notification()).isEqualTo(
            new TransactionAddedNotification("bob", bob.userId(), "alice", "Chez nous", groceries, finance));
        assertThat(requests.getLast().notification()).isEqualTo(
            new TransactionAddedNotification("carol", carol.userId(), "alice", "Chez nous", groceries, finance));
    }

    @Test
    void each_involved_reader_gets_their_variant() {
        adapter().transactionUpdated(gesture, groceries, groceries,
            List.of(new Involved(bob, Involvement.REMOVED), new Involved(carol, Involvement.ADDED)));

        List<NotificationRequest> requests = sent(2);
        assertThat(requests.getFirst().notification()).isEqualTo(new TransactionUpdatedNotification("bob", bob.userId(),
            "alice", "Chez nous", Involvement.REMOVED, groceries, groceries, finance));
        assertThat(requests.getLast().notification()).isEqualTo(new TransactionUpdatedNotification("carol", carol.userId(),
            "alice", "Chez nous", Involvement.ADDED, groceries, groceries, finance));
    }

    @Test
    void a_deleted_operation_and_the_three_series_events_reach_their_readers() {
        adapter().transactionDeleted(gesture, groceries, List.of(bob));
        adapter().recurringCreated(gesture, rent, List.of(bob));
        adapter().recurringUpdated(gesture, rent, rent, List.of(new Involved(bob, Involvement.CHANGED)));
        adapter().recurringStopped(gesture, rent, List.of(bob));

        assertThat(sent(4)).extracting(NotificationRequest::notification).containsExactly(
            new TransactionDeletedNotification("bob", bob.userId(), "alice", "Chez nous", groceries, finance),
            new RecurringCreatedNotification("bob", bob.userId(), "alice", "Chez nous", rent, finance),
            new RecurringUpdatedNotification("bob", bob.userId(), "alice", "Chez nous", Involvement.CHANGED, rent, rent, finance),
            new RecurringStoppedNotification("bob", bob.userId(), "alice", "Chez nous", rent, finance));
    }

    private final GoalFacts holidays = new GoalFacts("Vacances", new BigDecimal("1000.00"), null);

    private SavingsDeposit depositFrom(UUID contributor, String saved) {
        return new SavingsDeposit(holidays, contributor, "bob", new BigDecimal("100.00"), LocalDate.of(2026, 10, 21),
            new BigDecimal(saved), List.of());
    }

    @Test
    void each_reader_reads_the_side_of_the_deposit_they_stand_on() {
        SavingsDeposit forBob = depositFrom(bob.userId(), "700.00");

        adapter().savingsDeposited(gesture, forBob, List.of(bob, carol));

        assertThat(sent(2)).extracting(NotificationRequest::notification).containsExactly(
            new SavingsContributionNotification("bob", "alice", "Chez nous", ContributorSide.READER, forBob, finance),
            new SavingsContributionNotification("carol", "alice", "Chez nous", ContributorSide.OTHER_MEMBER, forBob, finance));
    }

    @Test
    void a_deposit_by_its_author_reads_as_theirs() {
        SavingsDeposit own = depositFrom(aliceId, "700.00");

        adapter().savingsDeposited(gesture, own, List.of(carol));

        assertThat(sent(1).getFirst().notification()).isEqualTo(
            new SavingsContributionNotification("carol", "alice", "Chez nous", ContributorSide.AUTHOR, own, finance));
    }

    @Test
    void a_deposit_that_reaches_the_goal_falls_back_on_the_plain_deposit() {
        SavingsDeposit last = depositFrom(aliceId, "1000.00");

        adapter().savingsDeposited(gesture, last, List.of(carol));

        NotificationRequest request = sent(1).getFirst();
        assertThat(request.recipientId()).isEqualTo(carol.userId());
        assertThat(request.notification()).isEqualTo(
            new SavingsGoalReachedNotification("carol", "alice", "Chez nous", ContributorSide.AUTHOR, last, finance));
        assertThat(request.fallback()).isEqualTo(
            new SavingsContributionNotification("carol", "alice", "Chez nous", ContributorSide.AUTHOR, last, finance));
    }

    @Test
    void a_deposit_short_of_the_goal_has_no_fallback() {
        adapter().savingsDeposited(gesture, depositFrom(aliceId, "999.99"), List.of(carol));

        assertThat(sent(1).getFirst().fallback()).isNull();
    }

    @Test
    void the_goal_events_reach_their_readers() {
        GoalFacts after = new GoalFacts("Vacances", new BigDecimal("1200.00"), null);

        adapter().savingsGoalCreated(gesture, holidays, List.of(bob));
        adapter().savingsGoalUpdated(gesture, holidays, after, List.of(bob));
        adapter().savingsGoalDeleted(gesture, holidays, new BigDecimal("300.00"), List.of(), List.of(bob));

        assertThat(sent(3)).extracting(NotificationRequest::notification).containsExactly(
            new SavingsGoalCreatedNotification("bob", "alice", "Chez nous", holidays, finance),
            new SavingsGoalUpdatedNotification("bob", "alice", "Chez nous", holidays, after, finance),
            new SavingsGoalDeletedNotification("bob", "alice", "Chez nous", holidays, new BigDecimal("300.00"), List.of(), finance));
    }

    @Test
    void a_repayment_reaches_the_other_party_only() {
        adapter().settlementRecorded(gesture, SettlementAuthor.DEBTOR, new BigDecimal("50.00"), LocalDate.of(2026, 10, 21),
            Standing.EVEN, BigDecimal.ZERO, bob);

        NotificationRequest request = sent(1).getFirst();
        assertThat(request.recipientId()).isEqualTo(bob.userId());
        assertThat(request.notification()).isEqualTo(new SettlementRecordedNotification("bob", "alice", "Chez nous",
            SettlementAuthor.DEBTOR, new BigDecimal("50.00"), LocalDate.of(2026, 10, 21), Standing.EVEN, BigDecimal.ZERO, finance));
    }

    @Test
    void the_opening_balance_set_or_removed_reaches_its_readers() {
        adapter().openingBalanceSet(gesture, new BigDecimal("1520.30"), LocalDate.of(2026, 10, 1), List.of(bob));
        adapter().openingBalanceCleared(gesture, List.of(carol));

        assertThat(sent(2)).extracting(NotificationRequest::notification).containsExactly(
            new OpeningBalanceChangedNotification("bob", "alice", "Chez nous", false, new BigDecimal("1520.30"),
                LocalDate.of(2026, 10, 1), finance),
            new OpeningBalanceChangedNotification("carol", "alice", "Chez nous", true, null, null, finance));
    }

    @Test
    void a_budget_crossing_says_which_threshold_it_reached() {
        BudgetAlert alert = new BudgetAlert("Alimentation", YearMonth.of(2026, 10), new BigDecimal("415.00"),
            new BigDecimal("500.00"), "Courses", new BigDecimal("35.00"));

        adapter().budgetCrossed(gesture, BudgetStatus.WARNING, alert, List.of(bob));
        adapter().budgetCrossed(gesture, BudgetStatus.OVER, alert, List.of(carol));

        assertThat(sent(2)).extracting(NotificationRequest::notification).containsExactly(
            new BudgetWarningNotification("bob", "alice", "Chez nous", alert, finance),
            new BudgetExceededNotification("carol", "alice", "Chez nous", alert, finance));
    }
}
