package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.Balances;
import com.nido.api.finance.domain.model.MemberBalance;
import com.nido.api.finance.domain.model.SuggestedTransfer;
import com.nido.api.finance.domain.port.out.RunningTotalsRepository;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetBalancesHandlerTest {

    @Mock RunningTotalsRepository runningTotals;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID aliceId = UUID.randomUUID();
    private final UUID bobId = UUID.randomUUID();

    @Test
    void settles_the_members_from_their_nets_over_all_months() {
        Map<UUID, BigDecimal> nets = new LinkedHashMap<>();
        nets.put(aliceId, new BigDecimal("20"));
        nets.put(bobId, new BigDecimal("-20"));
        when(runningTotals.findNetByMember(spaceId)).thenReturn(nets);
        SpaceMembership membership = new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());

        Balances balances = new GetBalancesHandler(runningTotals).getBalances(membership);

        assertThat(balances.netByMember()).containsExactlyInAnyOrder(
            new MemberBalance(aliceId, new BigDecimal("20.00")), new MemberBalance(bobId, new BigDecimal("-20.00")));
        assertThat(balances.suggestedTransfers()).containsExactly(new SuggestedTransfer(bobId, aliceId, new BigDecimal("20.00")));
    }
}
