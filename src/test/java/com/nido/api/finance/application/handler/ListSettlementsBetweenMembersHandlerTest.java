package com.nido.api.finance.application.handler;

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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListSettlementsBetweenMembersHandlerTest {

    @Mock SettlementRecordRepository settlementRecordRepository;
    private ListSettlementsBetweenMembersHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID aliceId = UUID.randomUUID();
    private final UUID bobId = UUID.randomUUID();
    private final UUID carolId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new ListSettlementsBetweenMembersHandler(settlementRecordRepository);
    }

    private SpaceMembership membership() {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());
    }

    @Test
    void returns_settlements_between_the_two_members_in_either_direction_newest_first() {
        SettlementRecord aliceToBob = new SettlementRecord(UUID.randomUUID(), spaceId, aliceId, bobId, new BigDecimal("100.00"), LocalDate.of(2026, 1, 5));
        SettlementRecord bobToAlice = new SettlementRecord(UUID.randomUUID(), spaceId, bobId, aliceId, new BigDecimal("30.00"), LocalDate.of(2026, 2, 1));
        SettlementRecord aliceToCarol = new SettlementRecord(UUID.randomUUID(), spaceId, aliceId, carolId, new BigDecimal("50.00"), LocalDate.of(2026, 1, 10));
        when(settlementRecordRepository.findBySpaceId(spaceId)).thenReturn(List.of(aliceToBob, bobToAlice, aliceToCarol));

        List<SettlementRecord> result = handler.list(aliceId, bobId, membership());

        assertThat(result).containsExactly(bobToAlice, aliceToBob);
    }

    @Test
    void returns_an_empty_list_when_the_two_members_never_settled_anything() {
        when(settlementRecordRepository.findBySpaceId(spaceId)).thenReturn(List.of());

        List<SettlementRecord> result = handler.list(aliceId, bobId, membership());

        assertThat(result).isEmpty();
    }
}
