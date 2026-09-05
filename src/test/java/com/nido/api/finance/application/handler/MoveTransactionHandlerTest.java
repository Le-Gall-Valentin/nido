package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.finance.domain.port.out.TransactionRepository;
import com.nido.api.space.application.port.in.ResolveMembershipUseCase;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoveTransactionHandlerTest {

    @Mock TransactionRepository transactionRepository;
    @Mock ResolveMembershipUseCase resolveMembershipUseCase;
    private MoveTransactionHandler handler;
    private final UUID sourceSpaceId = UUID.randomUUID();
    private final UUID destinationSpaceId = UUID.randomUUID();
    private final UUID transactionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new MoveTransactionHandler(transactionRepository, resolveMembershipUseCase);
    }

    private SpaceMembership sourceMembership() {
        return new SpaceMembership(UUID.randomUUID(), sourceSpaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());
    }

    @Test
    void moving_creates_the_transaction_in_the_destination_without_contributors_or_series_link_then_deletes_the_source() {
        UUID categoryId = UUID.randomUUID();
        SpaceMembership caller = sourceMembership();
        Transaction source = new Transaction(transactionId, sourceSpaceId, "Courses", new BigDecimal("10.00"),
            TransactionType.EXPENSE, categoryId, LocalDate.of(2026, 1, 1), UUID.randomUUID(), List.of(), null, Instant.now());
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(source));
        SpaceMembership destinationMembership = new SpaceMembership(UUID.randomUUID(), destinationSpaceId, UUID.randomUUID(), SpaceRole.OWNER, Instant.now());
        when(resolveMembershipUseCase.resolve(destinationSpaceId, caller.userId())).thenReturn(destinationMembership);
        Transaction moved = new Transaction(UUID.randomUUID(), destinationSpaceId, "Courses", new BigDecimal("10.00"),
            TransactionType.EXPENSE, categoryId, LocalDate.of(2026, 1, 1), null, List.of(), null, Instant.now());
        when(transactionRepository.create(any(), any())).thenReturn(moved);

        Transaction result = handler.move(transactionId, destinationSpaceId, caller);

        assertThat(result).isEqualTo(moved);
    }

    @Test
    void moving_into_the_same_space_is_rejected() {
        assertThatThrownBy(() -> handler.move(transactionId, sourceSpaceId, sourceMembership()))
            .isInstanceOf(FinanceException.SameSpaceTransfer.class);
    }
}
