package com.nido.api.finance.infrastructure.notification;

import com.nido.api.finance.domain.model.Gesture;
import com.nido.api.finance.domain.model.Involved;
import com.nido.api.finance.domain.model.Involvement;
import com.nido.api.finance.domain.model.MemberShare;
import com.nido.api.finance.domain.model.OperationFacts;
import com.nido.api.finance.domain.model.RecurrenceInterval;
import com.nido.api.finance.domain.model.SeriesFacts;
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
}
