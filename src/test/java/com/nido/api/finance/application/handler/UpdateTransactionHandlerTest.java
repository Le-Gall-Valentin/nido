package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.Contribution;
import com.nido.api.finance.domain.model.ContributionInput;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.finance.domain.model.UpdateTransactionCommand;
import com.nido.api.finance.domain.port.out.TransactionRepository;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateTransactionHandlerTest {

    @Mock TransactionRepository transactionRepository;
    private UpdateTransactionHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID aliceId = UUID.randomUUID();
    private final UUID bobId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new UpdateTransactionHandler(transactionRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    private Transaction existingInSameSpace(UUID transactionId) {
        return new Transaction(transactionId, spaceId, "T", new BigDecimal("20.00"), TransactionType.EXPENSE,
            UUID.randomUUID(), LocalDate.of(2026, 1, 2), null, List.of(), null, Instant.now());
    }

    @Test
    void a_member_can_update_a_transaction() {
        UpdateTransactionCommand command = new UpdateTransactionCommand(UUID.randomUUID(), spaceId, "T modifié",
            new BigDecimal("20.00"), TransactionType.EXPENSE, UUID.randomUUID(), LocalDate.of(2026, 1, 2), null, List.of());
        when(transactionRepository.findById(command.transactionId())).thenReturn(Optional.of(existingInSameSpace(command.transactionId())));
        Transaction updated = new Transaction(command.transactionId(), spaceId, "T modifié", new BigDecimal("20.00"),
            TransactionType.EXPENSE, command.categoryId(), command.date(), null, List.of(), null, Instant.now());
        when(transactionRepository.update(command, List.of())).thenReturn(updated);

        Transaction result = handler.update(command, membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(updated);
    }

    @Test
    void updating_a_transaction_that_belongs_to_a_different_space_is_rejected() {
        UUID transactionId = UUID.randomUUID();
        UUID otherSpaceId = UUID.randomUUID();
        UpdateTransactionCommand command = new UpdateTransactionCommand(transactionId, spaceId, "T modifié",
            new BigDecimal("20.00"), TransactionType.EXPENSE, UUID.randomUUID(), LocalDate.of(2026, 1, 2), null, List.of());
        Transaction foreign = new Transaction(transactionId, otherSpaceId, "T", new BigDecimal("20.00"), TransactionType.EXPENSE,
            UUID.randomUUID(), LocalDate.of(2026, 1, 2), null, List.of(), null, Instant.now());
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(FinanceException.TransactionNotFound.class);
        verify(transactionRepository, never()).update(any(), any());
    }

    @Test
    void updating_a_transaction_that_does_not_exist_is_rejected() {
        UUID transactionId = UUID.randomUUID();
        UpdateTransactionCommand command = new UpdateTransactionCommand(transactionId, spaceId, "T modifié",
            new BigDecimal("20.00"), TransactionType.EXPENSE, UUID.randomUUID(), LocalDate.of(2026, 1, 2), null, List.of());
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(FinanceException.TransactionNotFound.class);
    }

    @Test
    void resolves_custom_shares_that_sum_to_the_amount_before_calling_the_repository() {
        UpdateTransactionCommand command = new UpdateTransactionCommand(UUID.randomUUID(), spaceId, "T modifié",
            new BigDecimal("100.00"), TransactionType.EXPENSE, UUID.randomUUID(), LocalDate.of(2026, 1, 2), aliceId,
            List.of(new ContributionInput(aliceId, new BigDecimal("70.00")), new ContributionInput(bobId, new BigDecimal("30.00"))));
        when(transactionRepository.findById(command.transactionId())).thenReturn(Optional.of(existingInSameSpace(command.transactionId())));
        List<Contribution> expectedResolved = List.of(new Contribution(aliceId, new BigDecimal("70.00")), new Contribution(bobId, new BigDecimal("30.00")));
        Transaction updated = new Transaction(command.transactionId(), spaceId, "T modifié", new BigDecimal("100.00"),
            TransactionType.EXPENSE, command.categoryId(), command.date(), aliceId, expectedResolved, null, Instant.now());
        when(transactionRepository.update(command, expectedResolved)).thenReturn(updated);

        Transaction result = handler.update(command, membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(updated);
        verify(transactionRepository).update(command, expectedResolved);
    }

    @Test
    void updating_a_transaction_with_custom_shares_that_do_not_sum_to_the_amount_is_rejected() {
        UpdateTransactionCommand command = new UpdateTransactionCommand(UUID.randomUUID(), spaceId, "T modifié",
            new BigDecimal("100.00"), TransactionType.EXPENSE, UUID.randomUUID(), LocalDate.of(2026, 1, 2), aliceId,
            List.of(new ContributionInput(aliceId, new BigDecimal("40.00")), new ContributionInput(bobId, new BigDecimal("30.00"))));

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(FinanceException.InvalidContributionShares.class);
        verify(transactionRepository, never()).update(any(), any());
    }

    @Test
    void a_viewer_cannot_update_a_transaction() {
        UpdateTransactionCommand command = new UpdateTransactionCommand(UUID.randomUUID(), spaceId, "T modifié",
            new BigDecimal("20.00"), TransactionType.EXPENSE, UUID.randomUUID(), LocalDate.of(2026, 1, 2), null, List.of());

        assertThatThrownBy(() -> handler.update(command, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }
}
