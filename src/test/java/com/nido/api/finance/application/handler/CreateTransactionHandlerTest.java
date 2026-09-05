package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.Contribution;
import com.nido.api.finance.domain.model.ContributionInput;
import com.nido.api.finance.domain.model.CreateTransactionCommand;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.model.TransactionType;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateTransactionHandlerTest {

    @Mock TransactionRepository transactionRepository;
    private CreateTransactionHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID aliceId = UUID.randomUUID();
    private final UUID bobId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new CreateTransactionHandler(transactionRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    @Test
    void a_member_can_create_a_transaction_with_no_contributors() {
        CreateTransactionCommand command = new CreateTransactionCommand(spaceId, "Courses", new BigDecimal("45.30"),
            TransactionType.EXPENSE, UUID.randomUUID(), LocalDate.of(2026, 1, 15), null, List.of(), null);
        Transaction created = new Transaction(UUID.randomUUID(), spaceId, "Courses", new BigDecimal("45.30"),
            TransactionType.EXPENSE, command.categoryId(), command.date(), null, List.of(), null, Instant.now());
        when(transactionRepository.create(command, List.of())).thenReturn(created);

        Transaction result = handler.create(command, membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(created);
    }

    @Test
    void resolves_an_equal_split_before_calling_the_repository() {
        CreateTransactionCommand command = new CreateTransactionCommand(spaceId, "Courses", new BigDecimal("50.00"),
            TransactionType.EXPENSE, UUID.randomUUID(), LocalDate.of(2026, 1, 15), aliceId,
            List.of(new ContributionInput(aliceId, null), new ContributionInput(bobId, null)), null);
        List<Contribution> expectedResolved = List.of(new Contribution(aliceId, new BigDecimal("25.00")), new Contribution(bobId, new BigDecimal("25.00")));
        Transaction created = new Transaction(UUID.randomUUID(), spaceId, "Courses", new BigDecimal("50.00"),
            TransactionType.EXPENSE, command.categoryId(), command.date(), aliceId, expectedResolved, null, Instant.now());
        when(transactionRepository.create(command, expectedResolved)).thenReturn(created);

        Transaction result = handler.create(command, membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(created);
        verify(transactionRepository).create(command, expectedResolved);
    }

    @Test
    void creating_a_transaction_with_custom_shares_that_do_not_sum_to_the_amount_is_rejected() {
        CreateTransactionCommand command = new CreateTransactionCommand(spaceId, "Courses", new BigDecimal("50.00"),
            TransactionType.EXPENSE, UUID.randomUUID(), LocalDate.of(2026, 1, 15), aliceId,
            List.of(new ContributionInput(aliceId, new BigDecimal("40.00")), new ContributionInput(bobId, new BigDecimal("20.00"))), null);

        assertThatThrownBy(() -> handler.create(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(FinanceException.InvalidContributionShares.class);
        verify(transactionRepository, never()).create(any(), any());
    }

    @Test
    void creating_a_split_transaction_with_no_payer_is_rejected() {
        CreateTransactionCommand command = new CreateTransactionCommand(spaceId, "Courses", new BigDecimal("50.00"),
            TransactionType.EXPENSE, UUID.randomUUID(), LocalDate.of(2026, 1, 15), null,
            List.of(new ContributionInput(aliceId, null), new ContributionInput(bobId, null)), null);

        assertThatThrownBy(() -> handler.create(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(FinanceException.PayerRequired.class);
        verify(transactionRepository, never()).create(any(), any());
    }

    @Test
    void a_viewer_cannot_create_a_transaction() {
        CreateTransactionCommand command = new CreateTransactionCommand(spaceId, "Courses", new BigDecimal("45.30"),
            TransactionType.EXPENSE, UUID.randomUUID(), LocalDate.of(2026, 1, 15), null, List.of(), null);

        assertThatThrownBy(() -> handler.create(command, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }

    @Test
    void creating_a_transaction_for_another_space_than_the_callers_is_rejected() {
        CreateTransactionCommand command = new CreateTransactionCommand(UUID.randomUUID(), "Courses", new BigDecimal("45.30"),
            TransactionType.EXPENSE, UUID.randomUUID(), LocalDate.of(2026, 1, 15), null, List.of(), null);

        assertThatThrownBy(() -> handler.create(command, membership(SpaceRole.MEMBER)))
            .isInstanceOf(SpaceException.NotAMember.class);
    }
}
