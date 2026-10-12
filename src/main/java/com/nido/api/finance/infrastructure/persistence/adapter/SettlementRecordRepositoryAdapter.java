package com.nido.api.finance.infrastructure.persistence.adapter;

import com.nido.api.finance.domain.model.CreateSettlementCommand;
import com.nido.api.finance.domain.model.SettlementRecord;
import com.nido.api.finance.domain.port.out.SettlementRecordRepository;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceSettlementRecordEntity;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceSettlementRecordJpaRepository;
import com.nido.api.infrastructure.sealing.SpaceSealer;
import com.nido.api.infrastructure.sealing.SpaceSealers;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Component
public class SettlementRecordRepositoryAdapter implements SettlementRecordRepository {

    private final FinanceSettlementRecordJpaRepository settlements;
    private final SpaceSealers sealers;
    private final MonthTotalsWriter monthTotalsWriter;

    public SettlementRecordRepositoryAdapter(FinanceSettlementRecordJpaRepository settlements, SpaceSealers sealers,
                                             MonthTotalsWriter monthTotalsWriter) {
        this.settlements = settlements;
        this.sealers = sealers;
        this.monthTotalsWriter = monthTotalsWriter;
    }

    @Override
    public List<SettlementRecord> findBetweenMembers(UUID spaceId, UUID memberAId, UUID memberBId) {
        return settlements.findBetweenMembers(spaceId, memberAId, memberBId).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    @Transactional
    public void lockForSettlement(UUID spaceId, UUID memberAId, UUID memberBId) {
        // Order-independent: sort the two ids so settling A→B and B→A contend for the same key.
        UUID first = memberAId.compareTo(memberBId) <= 0 ? memberAId : memberBId;
        UUID second = memberAId.compareTo(memberBId) <= 0 ? memberBId : memberAId;
        settlements.lockForSettlement("finance-settle|" + spaceId + "|" + first + "|" + second);
    }

    @Override
    @Transactional
    public SettlementRecord create(CreateSettlementCommand command) {
        FinanceSettlementRecordEntity e = new FinanceSettlementRecordEntity();
        e.setSpaceId(command.spaceId());
        e.setFromUserId(command.fromMemberId());
        e.setToUserId(command.toMemberId());
        e.setAmountEncrypted(SealedAmounts.seal(sealers.forSpace(command.spaceId()), FinanceSettlementRecordEntity.AMOUNT, e.getId(),
            command.amount()));
        e.setSettledDate(command.date());
        FinanceSettlementRecordEntity saved = settlements.saveAndFlush(e);
        monthTotalsWriter.refresh(saved.getSpaceId(), List.of(YearMonth.from(saved.getSettledDate())));
        return toDomain(saved);
    }

    private SettlementRecord toDomain(FinanceSettlementRecordEntity e) {
        return opened(sealers.forSpace(e.getSpaceId()), e);
    }

    /** A stored settlement, its amount opened with the key of its space: the one place that does it. */
    static SettlementRecord opened(SpaceSealer sealer, FinanceSettlementRecordEntity e) {
        BigDecimal amount = SealedAmounts.open(sealer, FinanceSettlementRecordEntity.AMOUNT, e.getId(), e.getAmountEncrypted());
        return new SettlementRecord(e.getId(), e.getSpaceId(), e.getFromUserId(), e.getToUserId(), amount, e.getSettledDate());
    }
}
