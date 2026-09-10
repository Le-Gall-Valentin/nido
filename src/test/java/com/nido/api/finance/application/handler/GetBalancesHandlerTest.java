package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.Balances;
import com.nido.api.finance.domain.model.Contribution;
import com.nido.api.finance.domain.model.SplitTransaction;
import com.nido.api.finance.domain.model.MemberBalance;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetBalancesHandlerTest {

    @Mock TransactionRepository transactionRepository;
    @Mock SettlementRecordRepository settlementRecordRepository;
    private GetBalancesHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID aliceId = UUID.randomUUID();
    private final UUID bobId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new GetBalancesHandler(transactionRepository, settlementRecordRepository);
    }

    @Test
    void delegates_to_BalanceCalculator_over_every_transaction_and_settlement_in_the_space() {
        // Only payer, amount and shares: the balance fold never reads a label, and the
        // repository no longer decrypts one for it.
        SplitTransaction dinner = new SplitTransaction(aliceId, new BigDecimal("40.00"),
            List.of(new Contribution(aliceId, new BigDecimal("20.00")),
                    new Contribution(bobId, new BigDecimal("20.00"))), TransactionType.EXPENSE);
        when(transactionRepository.findSplitsBySpaceId(spaceId)).thenReturn(List.of(dinner));
        when(settlementRecordRepository.findBySpaceId(spaceId)).thenReturn(List.of());
        SpaceMembership membership = new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());

        Balances balances = handler.getBalances(membership);

        assertThat(balances.netByMember()).containsExactlyInAnyOrder(
            new MemberBalance(aliceId, new BigDecimal("20.00")), new MemberBalance(bobId, new BigDecimal("-20.00")));
    }
}
