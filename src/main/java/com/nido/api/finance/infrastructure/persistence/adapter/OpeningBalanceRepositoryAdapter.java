package com.nido.api.finance.infrastructure.persistence.adapter;

import com.nido.api.finance.domain.model.OpeningBalance;
import com.nido.api.finance.domain.port.out.OpeningBalanceRepository;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceOpeningBalanceEntity;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceOpeningBalanceJpaRepository;
import com.nido.api.infrastructure.sealing.SpaceSealers;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class OpeningBalanceRepositoryAdapter implements OpeningBalanceRepository {

    private final FinanceOpeningBalanceJpaRepository openingBalances;
    private final SpaceSealers sealers;

    public OpeningBalanceRepositoryAdapter(FinanceOpeningBalanceJpaRepository openingBalances, SpaceSealers sealers) {
        this.openingBalances = openingBalances;
        this.sealers = sealers;
    }

    @Override
    public Optional<OpeningBalance> findBySpaceId(UUID spaceId) {
        return openingBalances.findBySpaceId(spaceId).map(this::toDomain);
    }

    @Override
    @Transactional
    public OpeningBalance save(OpeningBalance openingBalance) {
        // Two first saves at once — a double click — would both find nothing and insert.
        lockForWrite(openingBalance.spaceId());
        FinanceOpeningBalanceEntity e = openingBalances.findBySpaceId(openingBalance.spaceId()).orElseGet(() -> {
            FinanceOpeningBalanceEntity created = new FinanceOpeningBalanceEntity();
            created.setSpaceId(openingBalance.spaceId());
            return created;
        });
        e.setAmountEncrypted(SealedAmounts.seal(sealers.forSpace(e.getSpaceId()), FinanceOpeningBalanceEntity.AMOUNT, e.getId(),
            openingBalance.amount()));
        e.setAsOf(openingBalance.asOf());
        return toDomain(openingBalances.saveAndFlush(e));
    }

    @Override
    @Transactional
    public void deleteBySpaceId(UUID spaceId) {
        // A save that already read the row would otherwise update a row this just deleted.
        lockForWrite(spaceId);
        openingBalances.deleteBySpaceId(spaceId);
        openingBalances.flush();
    }

    /** One write of a space's opening balance at a time, until the transaction ends. */
    private void lockForWrite(UUID spaceId) {
        openingBalances.lockForWrite("finance-opening|" + spaceId);
    }

    private OpeningBalance toDomain(FinanceOpeningBalanceEntity e) {
        return new OpeningBalance(e.getSpaceId(),
            SealedAmounts.open(sealers.forSpace(e.getSpaceId()), FinanceOpeningBalanceEntity.AMOUNT, e.getId(), e.getAmountEncrypted()),
            e.getAsOf());
    }
}
