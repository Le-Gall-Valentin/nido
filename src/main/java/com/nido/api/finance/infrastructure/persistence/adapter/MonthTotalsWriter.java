package com.nido.api.finance.infrastructure.persistence.adapter;

import com.nido.api.finance.domain.model.MonthTotals;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceMonthMemberTotalEntity;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceMonthTotalEntity;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceMonthMemberTotalJpaRepository;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceMonthTotalJpaRepository;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceSettlementRecordJpaRepository;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceTransactionContributorJpaRepository;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceTransactionJpaRepository;
import com.nido.api.infrastructure.sealing.SpaceSealer;
import com.nido.api.infrastructure.sealing.SpaceSealers;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Keeps finance_month_totals and finance_month_member_totals equal to what the operations and the settlements say.
 * Those stay the only source of truth: the two tables are a cache of them, folded again whenever in doubt.
 *
 * <p>Every write that changes an amount, a type, a date, a payer, a share or a settlement goes through
 * TransactionRepositoryAdapter or SettlementRecordRepositoryAdapter, and both call {@link #refresh} in the same
 * transaction. The months it names are folded again whole from their operations — never adjusted by a difference — so a
 * wrong total does not outlive the next write in its month. A value of the month that cannot be opened stops the write,
 * as it already stops the reading of that month.
 *
 * <p>The totals of a space are built at its first read rather than at start, where no hook may read a sealed value
 * (EncryptionBackfillRunner). {@link #TOTALS_VERSION} is the version they are built at: raising it rebuilds every space
 * at its next read, which is how a defect in their upkeep, if one is ever found, gets repaired.
 *
 * <p>Both run under the space's totals lock. A transaction takes it after any other finance lock it holds — the
 * materialization of recurring operations, a settlement between two members — and never before one, so no two
 * transactions wait on each other.
 *
 * <p>The refresh reads the month once it holds the lock, and sees what another writer committed meanwhile only because
 * each statement reads what is committed when it starts: READ COMMITTED, PostgreSQL's default and this application's.
 * Under REPEATABLE READ or SERIALIZABLE it would read the month as it was when its transaction began, and the totals
 * would drift.
 */
@Component
public class MonthTotalsWriter {

    static final int TOTALS_VERSION = 1;

    private final FinanceTransactionJpaRepository transactions;
    private final FinanceTransactionContributorJpaRepository contributors;
    private final FinanceSettlementRecordJpaRepository settlements;
    private final FinanceMonthTotalJpaRepository monthTotals;
    private final FinanceMonthMemberTotalJpaRepository memberTotals;
    private final SpaceSealers sealers;

    public MonthTotalsWriter(FinanceTransactionJpaRepository transactions, FinanceTransactionContributorJpaRepository contributors,
                             FinanceSettlementRecordJpaRepository settlements, FinanceMonthTotalJpaRepository monthTotals,
                             FinanceMonthMemberTotalJpaRepository memberTotals, SpaceSealers sealers) {
        this.transactions = transactions;
        this.contributors = contributors;
        this.settlements = settlements;
        this.monthTotals = monthTotals;
        this.memberTotals = memberTotals;
        this.sealers = sealers;
    }

    /** Folds these months of the space again from their operations and settlements. */
    @Transactional
    public void refresh(UUID spaceId, Collection<YearMonth> months) {
        lock(spaceId);
        SortedSet<YearMonth> distinct = new TreeSet<>(months);
        List<LocalDate> starts = distinct.stream().map(month -> month.atDay(1)).toList();
        monthTotals.deleteMonths(spaceId, starts);
        memberTotals.deleteMonths(spaceId, starts);
        SpaceSealer sealer = sealers.forSpace(spaceId);
        for (YearMonth month : distinct) {
            LocalDate from = month.atDay(1);
            LocalDate to = month.atEndOfMonth();
            Ledger.open(sealer, transactions.findLedgerRows(spaceId, from, to), contributors.findShareRows(spaceId, from, to),
                    settlements.findBySpaceIdAndSettledDateBetween(spaceId, from, to))
                .monthTotals()
                .forEach(totals -> write(spaceId, sealer, totals));
        }
    }

    /** Builds the space's totals when they were never built, or were built at an earlier version. */
    @Transactional
    public void ensureBuilt(UUID spaceId) {
        // Unlocked first: built totals are the case of every read but the first, and cost this one query.
        if (isCurrent(spaceId)) {
            return;
        }
        lock(spaceId);
        // Another read may have built them while this one waited for the lock.
        if (isCurrent(spaceId)) {
            return;
        }
        monthTotals.deleteAllOfSpace(spaceId);
        memberTotals.deleteAllOfSpace(spaceId);
        SpaceSealer sealer = sealers.forSpace(spaceId);
        Ledger.open(sealer, transactions.findLedgerRowsBySpaceId(spaceId), contributors.findShareRowsBySpaceId(spaceId),
                settlements.findBySpaceId(spaceId))
            .monthTotals()
            .forEach(totals -> write(spaceId, sealer, totals));
        monthTotals.saveTotalsVersion(spaceId, TOTALS_VERSION);
    }

    private boolean isCurrent(UUID spaceId) {
        return monthTotals.findTotalsVersion(spaceId).filter(version -> version >= TOTALS_VERSION).isPresent();
    }

    private void lock(UUID spaceId) {
        monthTotals.lockTotals("finance-totals|" + spaceId);
    }

    private void write(UUID spaceId, SpaceSealer sealer, MonthTotals totals) {
        LocalDate monthStart = totals.month().atDay(1);
        FinanceMonthTotalEntity net = new FinanceMonthTotalEntity();
        net.setSpaceId(spaceId);
        net.setMonthStart(monthStart);
        net.setNetEncrypted(SealedAmounts.seal(sealer, FinanceMonthTotalEntity.NET, net.getId(), totals.net()));
        monthTotals.saveAndFlush(net);
        memberTotals.saveAllAndFlush(totals.netByMember().entrySet().stream().map(entry -> {
            FinanceMonthMemberTotalEntity member = new FinanceMonthMemberTotalEntity();
            member.setSpaceId(spaceId);
            member.setMonthStart(monthStart);
            member.setMemberId(entry.getKey());
            member.setNetEncrypted(SealedAmounts.seal(sealer, FinanceMonthMemberTotalEntity.NET, member.getId(), entry.getValue()));
            return member;
        }).toList());
    }
}
