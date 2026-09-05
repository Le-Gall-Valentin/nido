package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.CreateSettlementCommand;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.SettlementRecord;
import com.nido.api.finance.domain.port.out.SettlementRecordRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettleDebtHandlerTest {

    @Mock SettlementRecordRepository settlementRecordRepository;
    private SettleDebtHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID debtorId = UUID.randomUUID();
    private final UUID creditorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new SettleDebtHandler(settlementRecordRepository);
    }

    private SpaceMembership membership(UUID userId, SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, userId, role, Instant.now());
    }

    private CreateSettlementCommand command() {
        return new CreateSettlementCommand(spaceId, debtorId, creditorId, new BigDecimal("20.00"), LocalDate.of(2026, 1, 2));
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
}
