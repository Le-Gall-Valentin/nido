package com.nido.api.finance.infrastructure.persistence.adapter;

import com.nido.api.finance.domain.model.Contribution;
import com.nido.api.finance.domain.model.MonthTotals;
import com.nido.api.finance.domain.model.SettlementRecord;
import com.nido.api.finance.domain.model.SplitTransaction;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceSettlementRecordEntity;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceTransactionContributorEntity;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceTransactionEntity;
import com.nido.api.finance.infrastructure.persistence.repository.ContributorShareRow;
import com.nido.api.finance.infrastructure.persistence.repository.LedgerRow;
import com.nido.api.infrastructure.sealing.SpaceSealer;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

/** Operations and settlements of a space, opened and ranged by month: what its MonthTotals are folded from. */
final class Ledger {

    private final Map<YearMonth, List<SplitTransaction>> transactionsByMonth;
    private final Map<YearMonth, List<SettlementRecord>> settlementsByMonth;

    private Ledger(Map<YearMonth, List<SplitTransaction>> transactionsByMonth,
                   Map<YearMonth, List<SettlementRecord>> settlementsByMonth) {
        this.transactionsByMonth = transactionsByMonth;
        this.settlementsByMonth = settlementsByMonth;
    }

    /** Opens the amounts, the shares and the settlements; never a label. */
    static Ledger open(SpaceSealer sealer, List<LedgerRow> rows, List<ContributorShareRow> shares,
                       List<FinanceSettlementRecordEntity> settlements) {
        Map<UUID, List<Contribution>> sharesByTransaction = shares.stream().collect(Collectors.groupingBy(
            ContributorShareRow::transactionId,
            Collectors.mapping(share -> new Contribution(share.userId(), SealedAmounts.open(sealer,
                FinanceTransactionContributorEntity.SHARE_AMOUNT, share.id(), share.shareAmountEncrypted())), Collectors.toList())));
        Map<YearMonth, List<SplitTransaction>> transactionsByMonth = rows.stream().collect(Collectors.groupingBy(
            row -> YearMonth.from(row.date()), TreeMap::new,
            Collectors.mapping(row -> new SplitTransaction(row.payerId(),
                SealedAmounts.open(sealer, FinanceTransactionEntity.AMOUNT, row.transactionId(), row.amountEncrypted()),
                sharesByTransaction.getOrDefault(row.transactionId(), List.of()), row.type()), Collectors.toList())));
        Map<YearMonth, List<SettlementRecord>> settlementsByMonth = settlements.stream().collect(Collectors.groupingBy(
            e -> YearMonth.from(e.getSettledDate()), TreeMap::new,
            Collectors.mapping(e -> SettlementRecordRepositoryAdapter.opened(sealer, e), Collectors.toList())));
        return new Ledger(transactionsByMonth, settlementsByMonth);
    }

    /** One per month holding an operation or a settlement, oldest first. */
    List<MonthTotals> monthTotals() {
        SortedSet<YearMonth> months = new TreeSet<>(transactionsByMonth.keySet());
        months.addAll(settlementsByMonth.keySet());
        return months.stream()
            .map(month -> MonthTotals.of(month, transactionsByMonth.getOrDefault(month, List.of()),
                settlementsByMonth.getOrDefault(month, List.of())))
            .toList();
    }
}
