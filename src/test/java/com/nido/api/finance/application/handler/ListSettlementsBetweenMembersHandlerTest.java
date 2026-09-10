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

    @BeforeEach
    void setUp() {
        handler = new ListSettlementsBetweenMembersHandler(settlementRecordRepository);
    }

    private SpaceMembership membership() {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());
    }

    @Test
    void asks_the_repository_for_the_pair_in_the_caller_s_own_space() {
        // Filtering by pair, either direction, newest first is now the database's job — verified
        // against real SQL in SettlementRecordRepositoryAdapterIT, where a mock could not lie
        // about it. What is left to check here is that the handler scopes the question to the
        // space the caller proved membership of, and passes the result through untouched.
        SettlementRecord aliceToBob = new SettlementRecord(UUID.randomUUID(), spaceId, aliceId, bobId,
            new BigDecimal("100.00"), LocalDate.of(2026, 1, 5));
        when(settlementRecordRepository.findBetweenMembers(spaceId, aliceId, bobId)).thenReturn(List.of(aliceToBob));

        List<SettlementRecord> result = handler.list(aliceId, bobId, membership());

        assertThat(result).containsExactly(aliceToBob);
    }

}
