package com.nido.api.finance.domain.port.out;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;

/**
 * The totals a space's balance and its members' balances are read from, a dozen a year — see MonthTotals. Built from
 * the operations and settlements at the space's first read, kept up to date by every write after that.
 */
public interface RunningTotalsRepository {

    /** The net of each month holding an operation or a settlement, oldest first. */
    Map<YearMonth, BigDecimal> findNetByMonth(UUID spaceId);

    /** Each member's net over all time, settlements included, unrounded — what BalanceCalculator.fromNets takes. */
    Map<UUID, BigDecimal> findNetByMember(UUID spaceId);
}
