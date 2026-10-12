package com.nido.api.finance.infrastructure.persistence.adapter;

import com.nido.api.finance.domain.port.out.RunningTotalsRepository;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceMonthMemberTotalEntity;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceMonthTotalEntity;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceMonthMemberTotalJpaRepository;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceMonthTotalJpaRepository;
import com.nido.api.finance.infrastructure.persistence.repository.MemberNetRow;
import com.nido.api.finance.infrastructure.persistence.repository.MonthNetRow;
import com.nido.api.infrastructure.sealing.SpaceSealer;
import com.nido.api.infrastructure.sealing.SpaceSealers;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Component
public class RunningTotalsRepositoryAdapter implements RunningTotalsRepository {

    private final MonthTotalsWriter writer;
    private final FinanceMonthTotalJpaRepository monthTotals;
    private final FinanceMonthMemberTotalJpaRepository memberTotals;
    private final SpaceSealers sealers;

    public RunningTotalsRepositoryAdapter(MonthTotalsWriter writer, FinanceMonthTotalJpaRepository monthTotals,
                                          FinanceMonthMemberTotalJpaRepository memberTotals, SpaceSealers sealers) {
        this.writer = writer;
        this.monthTotals = monthTotals;
        this.memberTotals = memberTotals;
        this.sealers = sealers;
    }

    /** Not read-only: the first read of a space builds its totals. */
    @Override
    @Transactional
    public Map<YearMonth, BigDecimal> findNetByMonth(UUID spaceId) {
        writer.ensureBuilt(spaceId);
        SpaceSealer sealer = sealers.forSpace(spaceId);
        Map<YearMonth, BigDecimal> nets = new TreeMap<>();
        for (MonthNetRow row : monthTotals.findNetRows(spaceId)) {
            nets.put(YearMonth.from(row.monthStart()),
                SealedAmounts.open(sealer, FinanceMonthTotalEntity.NET, row.id(), row.netEncrypted()));
        }
        return nets;
    }

    @Override
    @Transactional
    public Map<UUID, BigDecimal> findNetByMember(UUID spaceId) {
        writer.ensureBuilt(spaceId);
        SpaceSealer sealer = sealers.forSpace(spaceId);
        Map<UUID, BigDecimal> nets = new LinkedHashMap<>();
        for (MemberNetRow row : memberTotals.findNetRows(spaceId)) {
            nets.merge(row.memberId(),
                SealedAmounts.open(sealer, FinanceMonthMemberTotalEntity.NET, row.id(), row.netEncrypted()), BigDecimal::add);
        }
        return nets;
    }
}
