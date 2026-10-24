package com.nido.api.finance.application.service;

import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.Contribution;
import com.nido.api.finance.domain.model.Gesture;
import com.nido.api.finance.domain.model.Involved;
import com.nido.api.finance.domain.model.Involvement;
import com.nido.api.finance.domain.model.MemberShare;
import com.nido.api.finance.domain.model.OperationFacts;
import com.nido.api.finance.domain.model.RecurrenceInterval;
import com.nido.api.finance.domain.model.RecurringTransactionSeries;
import com.nido.api.finance.domain.model.SeriesFacts;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.finance.domain.port.out.CategoryRepository;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/** Who hears about an operation or a recurring transaction: whoever it concerns, never its author. */
@ExtendWith(MockitoExtension.class)
class OperationNotifierTest {

    @Mock ListSpaceMembersUseCase members;
    @Mock GetSpaceUseCase spaces;
    @Mock CategoryRepository categories;
    @Mock FinanceNotificationPort port;

    private final UUID spaceId = UUID.randomUUID();
    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();
    private final UUID carol = UUID.randomUUID();
    private final UUID formerMember = UUID.randomUUID();
    private final UUID groceries = UUID.randomUUID();
    private final SpaceMembership aliceActs = new SpaceMembership(UUID.randomUUID(), spaceId, alice, SpaceRole.MEMBER, Instant.now());
    private final Gesture gesture = new Gesture(spaceId, "Chez nous", alice, "alice");
    private final Addressee bobReads = new Addressee(bob, "bob");
    private final Addressee carolReads = new Addressee(carol, "carol");

    private OperationNotifier notifier;

    @BeforeEach
    void setUp() {
        lenient().when(members.list(aliceActs)).thenReturn(List.of(member(alice, "alice"), member(bob, "bob"), member(carol, "carol")));
        lenient().when(spaces.get(spaceId, aliceActs)).thenReturn(new SpaceDetailView(spaceId, SpaceType.SHARED, "Chez nous",
            null, "#c17a5c", "🏡", ZoneId.of("Europe/Paris"), SpaceRole.MEMBER, 3));
        lenient().when(categories.findById(groceries)).thenReturn(Optional.of(
            new Category(groceries, spaceId, "Alimentation", "#f59e0b", "Utensils", true, TransactionType.EXPENSE)));
        notifier = new OperationNotifier(new FinanceAudience(members, spaces), new FinanceFacts(categories), port);
    }

    private static SpaceMemberView member(UUID id, String name) {
        return new SpaceMemberView(id, name, name + "@test.local", SpaceRole.MEMBER, Instant.now());
    }

    private Transaction operation(String amount, UUID payer, UUID... sharedBy) {
        BigDecimal share = sharedBy.length == 0 ? BigDecimal.ZERO
            : new BigDecimal(amount).divide(BigDecimal.valueOf(sharedBy.length));
        return new Transaction(UUID.randomUUID(), spaceId, "Courses", new BigDecimal(amount), TransactionType.EXPENSE, groceries,
            LocalDate.of(2026, 10, 21), payer, Arrays.stream(sharedBy).map(id -> new Contribution(id, share)).toList(), null,
            Instant.now());
    }

    private Transaction changed(Transaction t, String amount, UUID payer, UUID... sharedBy) {
        Transaction other = operation(amount, payer, sharedBy);
        return new Transaction(t.id(), spaceId, t.label(), other.amount(), t.type(), t.categoryId(), t.date(), payer,
            other.contributors(), null, t.createdAt());
    }

    private RecurringTransactionSeries rent(String amount, UUID payer, UUID... sharedBy) {
        BigDecimal share = new BigDecimal(amount).divide(BigDecimal.valueOf(sharedBy.length));
        return new RecurringTransactionSeries(UUID.randomUUID(), spaceId, "Loyer", new BigDecimal(amount), TransactionType.EXPENSE,
            groceries, payer, Arrays.stream(sharedBy).map(id -> new Contribution(id, share)).toList(),
            RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 11, 1), null, null);
    }

    @Test
    void an_added_operation_tells_its_payer_and_contributors_but_not_its_author() {
        notifier.transactionAdded(aliceActs, operation("86.40", alice, alice, bob));

        verify(port).transactionAdded(gesture, new OperationFacts("Courses", new BigDecimal("86.40"), TransactionType.EXPENSE,
            "Alimentation", LocalDate.of(2026, 10, 21), alice, "alice",
            List.of(new MemberShare(alice, "alice", new BigDecimal("43.20")), new MemberShare(bob, "bob", new BigDecimal("43.20")))),
            List.of(bobReads));
    }

    @Test
    void a_payer_named_by_someone_else_is_told() {
        notifier.transactionAdded(aliceActs, operation("60.00", bob));

        verify(port).transactionAdded(any(), any(), eq(List.of(bobReads)));
    }

    @Test
    void an_unshared_operation_tells_nobody() {
        notifier.transactionAdded(aliceActs, operation("60.00", null));

        verifyNoInteractions(port, members, spaces);
    }

    @Test
    void a_gesture_that_concerns_only_its_author_reads_no_member() {
        notifier.transactionAdded(aliceActs, operation("60.00", alice, alice));

        verifyNoInteractions(port, members, spaces, categories);
    }

    @Test
    void a_former_member_is_told_nothing_and_shown_as_former() {
        notifier.transactionAdded(aliceActs, operation("90.00", alice, formerMember, bob));

        verify(port).transactionAdded(gesture, new OperationFacts("Courses", new BigDecimal("90.00"), TransactionType.EXPENSE,
            "Alimentation", LocalDate.of(2026, 10, 21), alice, "alice",
            List.of(new MemberShare(formerMember, null, new BigDecimal("45.00")), new MemberShare(bob, "bob", new BigDecimal("45.00")))),
            List.of(bobReads));
    }

    @Test
    void an_update_that_changes_nothing_tells_nobody() {
        Transaction before = operation("86.40", alice, alice, bob);

        notifier.transactionUpdated(aliceActs, before, changed(before, "86.4", alice, alice, bob));

        verifyNoInteractions(port, members, spaces);
    }

    @Test
    void an_update_tells_removed_added_and_remaining_readers_their_variant() {
        Transaction before = operation("90.00", alice, alice, bob);
        Transaction after = changed(before, "90.00", alice, alice, carol);

        notifier.transactionUpdated(aliceActs, before, after);

        verify(port).transactionUpdated(eq(gesture), any(), any(),
            eq(List.of(new Involved(bobReads, Involvement.REMOVED), new Involved(carolReads, Involvement.ADDED))));
    }

    @Test
    void an_update_tells_whoever_stays_concerned_that_it_changed() {
        Transaction before = operation("80.00", alice, alice, bob);

        notifier.transactionUpdated(aliceActs, before, changed(before, "86.40", alice, alice, bob));

        verify(port).transactionUpdated(eq(gesture), any(), any(),
            eq(List.of(new Involved(bobReads, Involvement.CHANGED))));
    }

    @Test
    void a_deleted_operation_tells_who_it_concerned() {
        notifier.transactionDeleted(aliceActs, operation("86.40", bob, bob, carol));

        verify(port).transactionDeleted(eq(gesture), any(),
            eq(List.of(bobReads, carolReads)));
    }

    @Test
    void a_new_recurring_transaction_tells_whoever_it_concerns() {
        notifier.recurringCreated(aliceActs, rent("1200.00", alice, alice, bob));

        verify(port).recurringCreated(gesture, new SeriesFacts("Loyer", new BigDecimal("1200.00"), TransactionType.EXPENSE,
            "Alimentation", alice, "alice",
            List.of(new MemberShare(alice, "alice", new BigDecimal("600.00")), new MemberShare(bob, "bob", new BigDecimal("600.00"))),
            RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 11, 1), null), List.of(bobReads));
    }

    @Test
    void a_changed_recurring_transaction_tells_each_reader_their_variant_and_an_unchanged_one_nobody() {
        RecurringTransactionSeries before = rent("1200.00", alice, alice, bob);
        RecurringTransactionSeries after = new RecurringTransactionSeries(before.id(), spaceId, "Loyer", before.amount(),
            before.type(), groceries, alice, rent("1200.00", alice, alice, carol).contributors(), RecurrenceInterval.MONTHLY, 1,
            before.anchorDate(), null, LocalDate.of(2026, 11, 1));

        notifier.recurringUpdated(aliceActs, before, after);
        notifier.recurringUpdated(aliceActs, before, before);

        verify(port).recurringUpdated(eq(gesture), any(), any(),
            eq(List.of(new Involved(bobReads, Involvement.REMOVED), new Involved(carolReads, Involvement.ADDED))));
        verifyNoMoreInteractions(port);
    }

    @Test
    void a_stopped_recurring_transaction_tells_who_it_concerned() {
        notifier.recurringStopped(aliceActs, rent("1200.00", bob, bob, carol));

        verify(port).recurringStopped(eq(gesture), any(),
            eq(List.of(bobReads, carolReads)));
    }
}
