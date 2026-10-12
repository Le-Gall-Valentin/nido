package com.nido.api.finance.infrastructure.persistence.adapter;

import com.nido.api.finance.infrastructure.persistence.repository.FinanceOpeningBalanceJpaRepository;
import com.nido.api.infrastructure.sealing.SpaceSealers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class OpeningBalanceRepositoryAdapterTest {

    @Mock FinanceOpeningBalanceJpaRepository openingBalances;
    @Mock SpaceSealers sealers;

    @Test
    void a_removal_takes_the_lock_of_a_save_before_deleting() {
        // Without it, a save that already read the row would update a row a removal just deleted: a failed save.
        UUID spaceId = UUID.randomUUID();

        new OpeningBalanceRepositoryAdapter(openingBalances, sealers).deleteBySpaceId(spaceId);

        InOrder order = inOrder(openingBalances);
        order.verify(openingBalances).lockForWrite("finance-opening|" + spaceId);
        order.verify(openingBalances).deleteBySpaceId(spaceId);
    }
}
