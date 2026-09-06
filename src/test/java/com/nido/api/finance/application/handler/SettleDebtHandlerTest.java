package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.service.SpaceMemberValidator;
import com.nido.api.finance.domain.model.Contribution;
import com.nido.api.finance.domain.model.CreateSettlementCommand;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.SettlementRecord;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.finance.domain.port.out.SettlementRecordRepository;
import com.nido.api.finance.domain.port.out.TransactionRepository;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettleDebtHandlerTest {

    @Mock SettlementRecordRepository settlementRecordRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock SpaceMemberValidator spaceMemberValidator;
    private SettleDebtHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID debtorId = UUID.randomUUID();
    private final UUID creditorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new SettleDebtHandler(settlementRecordRepository, transactionRepository, spaceMemberValidator);
        // A debt of 1000.00 from debtorId to creditorId — large enough that tests not
        // concerned with the debt cap itself (amount 20.00) never trip over it.
        lenient().when(transactionRepository.findAllBySpaceId(spaceId)).thenReturn(List.of(sharedExpense(creditorId, debtorId, new BigDecimal("1000.00"))));
        lenient().when(settlementRecordRepository.findBySpaceId(spaceId)).thenReturn(List.of());
    }

    private SpaceMembership membership(UUID userId, SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, userId, role, Instant.now());
    }

    private CreateSettlementCommand command() {
        return new CreateSettlementCommand(spaceId, debtorId, creditorId, new BigDecimal("20.00"), LocalDate.of(2026, 1, 2));
    }

    private CreateSettlementCommand command(BigDecimal amount) {
        return new CreateSettlementCommand(spaceId, debtorId, creditorId, amount, LocalDate.of(2026, 1, 2));
    }

    /** A transaction {@code payerId} paid in full, with {@code contributorId} owing the whole {@code amount} back. */
    private Transaction sharedExpense(UUID payerId, UUID contributorId, BigDecimal amount) {
        return new Transaction(UUID.randomUUID(), spaceId, "T", amount, TransactionType.EXPENSE, UUID.randomUUID(),
            LocalDate.of(2026, 1, 1), payerId, List.of(new Contribution(contributorId, amount)), null, Instant.now());
    }

    @Test
    void the_debtor_can_record_a_settlement_even_as_a_viewer() {
        CreateSettlementCommand command = command();
        SettlementRecord created = new SettlementRecord(UUID.randomUUID(), spaceId, command.fromMemberId(), command.toMemberId(), command.amount(), command.date());
        when(settlementRecordRepository.create(command)).thenReturn(created);

        SettlementRecord result = handler.settle(command, membership(debtorId, SpaceRole.VIEWER));

        assertThat(result).isEqualTo(created);
    }

    @Test
    void the_creditor_can_record_a_settlement_even_as_a_viewer() {
        CreateSettlementCommand command = command();
        SettlementRecord created = new SettlementRecord(UUID.randomUUID(), spaceId, command.fromMemberId(), command.toMemberId(), command.amount(), command.date());
        when(settlementRecordRepository.create(command)).thenReturn(created);

        SettlementRecord result = handler.settle(command, membership(creditorId, SpaceRole.VIEWER));

        assertThat(result).isEqualTo(created);
    }

    @Test
    void a_third_member_cannot_record_a_settlement_between_two_other_members_even_with_write_access() {
        CreateSettlementCommand command = command();

        assertThatThrownBy(() -> handler.settle(command, membership(UUID.randomUUID(), SpaceRole.ADMIN)))
            .isInstanceOf(FinanceException.NotAPartyToSettlement.class);
    }

    @Test
    void validates_both_the_debtor_and_the_creditor_belong_to_the_space() {
        CreateSettlementCommand command = command();
        SettlementRecord created = new SettlementRecord(UUID.randomUUID(), spaceId, command.fromMemberId(), command.toMemberId(), command.amount(), command.date());
        when(settlementRecordRepository.create(command)).thenReturn(created);

        handler.settle(command, membership(debtorId, SpaceRole.VIEWER));

        verify(spaceMemberValidator).ensureMember(spaceId, debtorId);
        verify(spaceMemberValidator).ensureMember(spaceId, creditorId);
    }

    @Test
    void rejects_a_settlement_whose_counterparty_is_not_actually_a_member_of_the_space() {
        CreateSettlementCommand command = command();
        lenient().doNothing().when(spaceMemberValidator).ensureMember(spaceId, debtorId);
        doThrow(new FinanceException.MemberNotInSpace()).when(spaceMemberValidator).ensureMember(spaceId, creditorId);

        assertThatThrownBy(() -> handler.settle(command, membership(debtorId, SpaceRole.VIEWER)))
            .isInstanceOf(FinanceException.MemberNotInSpace.class);
    }

    @Test
    void allows_a_settlement_amount_up_to_exactly_the_real_debt() {
        when(transactionRepository.findAllBySpaceId(spaceId)).thenReturn(List.of(sharedExpense(creditorId, debtorId, new BigDecimal("100.00"))));
        CreateSettlementCommand command = command(new BigDecimal("100.00"));
        SettlementRecord created = new SettlementRecord(UUID.randomUUID(), spaceId, debtorId, creditorId, command.amount(), command.date());
        when(settlementRecordRepository.create(command)).thenReturn(created);

        SettlementRecord result = handler.settle(command, membership(debtorId, SpaceRole.VIEWER));

        assertThat(result).isEqualTo(created);
    }

    @Test
    void rejects_a_settlement_amount_exceeding_the_real_debt_between_the_two_members() {
        when(transactionRepository.findAllBySpaceId(spaceId)).thenReturn(List.of(sharedExpense(creditorId, debtorId, new BigDecimal("100.00"))));
        CreateSettlementCommand command = command(new BigDecimal("100.01"));

        assertThatThrownBy(() -> handler.settle(command, membership(debtorId, SpaceRole.VIEWER)))
            .isInstanceOf(FinanceException.SettlementExceedsDebt.class);
        verify(settlementRecordRepository, org.mockito.Mockito.never()).create(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void a_settlement_already_recorded_reduces_how_much_more_can_be_settled() {
        when(transactionRepository.findAllBySpaceId(spaceId)).thenReturn(List.of(sharedExpense(creditorId, debtorId, new BigDecimal("100.00"))));
        when(settlementRecordRepository.findBySpaceId(spaceId)).thenReturn(List.of(
            new SettlementRecord(UUID.randomUUID(), spaceId, debtorId, creditorId, new BigDecimal("60.00"), LocalDate.of(2026, 1, 1))));
        CreateSettlementCommand command = command(new BigDecimal("40.00"));
        SettlementRecord created = new SettlementRecord(UUID.randomUUID(), spaceId, debtorId, creditorId, command.amount(), command.date());
        when(settlementRecordRepository.create(command)).thenReturn(created);

        SettlementRecord result = handler.settle(command, membership(debtorId, SpaceRole.VIEWER));

        assertThat(result).isEqualTo(created);
    }

    @Test
    void rejects_a_settlement_when_there_is_no_debt_at_all_between_the_two_members() {
        when(transactionRepository.findAllBySpaceId(spaceId)).thenReturn(List.of());
        CreateSettlementCommand command = command(new BigDecimal("0.01"));

        assertThatThrownBy(() -> handler.settle(command, membership(debtorId, SpaceRole.VIEWER)))
            .isInstanceOf(FinanceException.SettlementExceedsDebt.class);
    }

    @Test
    void takes_the_per_pair_settlement_lock_before_recomputing_the_real_debt() {
        CreateSettlementCommand command = command();
        SettlementRecord created = new SettlementRecord(UUID.randomUUID(), spaceId, command.fromMemberId(), command.toMemberId(), command.amount(), command.date());
        when(settlementRecordRepository.create(command)).thenReturn(created);

        handler.settle(command, membership(debtorId, SpaceRole.VIEWER));

        verify(settlementRecordRepository).lockForSettlement(spaceId, debtorId, creditorId);
    }
}
