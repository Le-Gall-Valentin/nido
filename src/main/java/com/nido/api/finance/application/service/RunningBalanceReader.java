package com.nido.api.finance.application.service;

import com.nido.api.finance.domain.model.RunningBalance;
import com.nido.api.finance.domain.port.out.OpeningBalanceRepository;
import com.nido.api.finance.domain.port.out.RunningTotalsRepository;
import com.nido.api.finance.domain.port.out.TransactionRepository;
import com.nido.api.shared.annotation.ApplicationService;

import java.time.YearMonth;
import java.util.UUID;

/**
 * The balance of a space at any day: its month totals, anchored on its opening balance when it has one. Reads a dozen
 * figures a year, and one month of amounts more when there is an opening balance — never the whole history.
 */
@ApplicationService
public class RunningBalanceReader {

    private final RunningTotalsRepository runningTotals;
    private final OpeningBalanceRepository openingBalances;
    private final TransactionRepository transactions;

    public RunningBalanceReader(RunningTotalsRepository runningTotals, OpeningBalanceRepository openingBalances,
                                TransactionRepository transactions) {
        this.runningTotals = runningTotals;
        this.openingBalances = openingBalances;
        this.transactions = transactions;
    }

    public RunningBalance read(UUID spaceId) {
        RunningBalance running = RunningBalance.of(runningTotals.findNetByMonth(spaceId));
        return openingBalances.findBySpaceId(spaceId)
            .map(opening -> running.anchoredAt(opening,
                transactions.sumRecordedBetween(spaceId, YearMonth.from(opening.asOf()).atDay(1), opening.asOf())))
            .orElse(running);
    }
}
