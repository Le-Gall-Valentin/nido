package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.CreateSettlementCommand;
import com.nido.api.finance.domain.model.SettlementRecord;
import com.nido.api.finance.domain.port.out.SettlementRecordRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettleDebtHandlerTest {

    @Mock SettlementRecordRepository settlementRecordRepository;
    private SettleDebtHandler handler;
    private final UUID spaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new SettleDebtHandler(settlementRecordRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    @Test
    void a_member_can_record_a_settlement() {
        CreateSettlementCommand command = new CreateSettlementCommand(spaceId, UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("20.00"), LocalDate.of(2026, 1, 2));
        SettlementRecord created = new SettlementRecord(UUID.randomUUID(), spaceId, command.fromMemberId(), command.toMemberId(), command.amount(), command.date());
        when(settlementRecordRepository.create(command)).thenReturn(created);

        SettlementRecord result = handler.settle(command, membership(SpaceRole.MEMBER));

        assertThat(result).isEqualTo(created);
    }

    @Test
    void a_viewer_cannot_record_a_settlement() {
        CreateSettlementCommand command = new CreateSettlementCommand(spaceId, UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("20.00"), LocalDate.of(2026, 1, 2));

        assertThatThrownBy(() -> handler.settle(command, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }
}
