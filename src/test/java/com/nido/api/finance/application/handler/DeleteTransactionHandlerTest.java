package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.finance.domain.port.out.TransactionRepository;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.finance.application.service.OperationNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DeleteTransactionHandlerTest {

    @Mock TransactionRepository transactionRepository;
    @Mock OperationNotifier notifier;
    private DeleteTransactionHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID transactionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new DeleteTransactionHandler(transactionRepository, notifier);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    private Transaction oneOffTransaction() {
        return new Transaction(transactionId, spaceId, "T", new BigDecimal("10.00"), TransactionType.EXPENSE,
            UUID.randomUUID(), LocalDate.of(2026, 1, 1), null, List.of(), null, Instant.now());
    }

    @Test
    void a_member_can_delete_a_one_off_transaction() {
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(oneOffTransaction()));

        handler.delete(transactionId, spaceId, membership(SpaceRole.MEMBER));

        verify(transactionRepository).delete(transactionId);
    }

    @Test
    void a_member_can_delete_a_past_occurrence_materialized_by_a_recurring_series() {
        // Deleting one materialized occurrence doesn't affect the series' own cursor
        // (lastMaterializedDate) or bring the row back — see RecurringTransactionMaterializer.
        // Canceling the series itself instead is DeleteRecurringSeriesHandler's job.
        Transaction recurring = new Transaction(transactionId, spaceId, "T", new BigDecimal("10.00"), TransactionType.EXPENSE,
            UUID.randomUUID(), LocalDate.of(2026, 1, 1), null, List.of(), UUID.randomUUID(), Instant.now());
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(recurring));

        handler.delete(transactionId, spaceId, membership(SpaceRole.MEMBER));

        verify(transactionRepository).delete(transactionId);
    }

    @Test
    void a_viewer_cannot_delete_a_transaction() {
        assertThatThrownBy(() -> handler.delete(transactionId, spaceId, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }

    @Test
    void a_deleted_transaction_tells_the_notifier_what_it_was_after_deleting_it() {
        Transaction existing = oneOffTransaction();
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(existing));
        SpaceMembership caller = membership(SpaceRole.MEMBER);

        handler.delete(transactionId, spaceId, caller);

        InOrder order = inOrder(transactionRepository, notifier);
        order.verify(transactionRepository).delete(transactionId);
        order.verify(notifier).transactionDeleted(caller, existing);
    }

    @Test
    void a_refused_gesture_tells_nobody() {
        assertThatThrownBy(() -> handler.delete(transactionId, spaceId, membership(SpaceRole.VIEWER)));
        verifyNoInteractions(notifier);
    }
}
