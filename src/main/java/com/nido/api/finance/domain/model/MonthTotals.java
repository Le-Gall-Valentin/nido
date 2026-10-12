package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * What one month of a space adds up to: its net — income minus expenses, every operation counted, shared or not — and
 * the movement of each member's balance — shared operations and settlements, as {@link BalanceCalculator#fold} counts
 * them. Kept month by month so that a balance is read from a dozen figures a year rather than from every operation ever
 * recorded.
 */
public record MonthTotals(YearMonth month, BigDecimal net, Map<UUID, BigDecimal> netByMember) {

    public MonthTotals {
        Objects.requireNonNull(month, "month");
        Objects.requireNonNull(net, "net");
        netByMember = Collections.unmodifiableMap(new LinkedHashMap<>(netByMember));
    }

    /** @param transactions every operation of the month, those nobody shares included */
    public static MonthTotals of(YearMonth month, List<SplitTransaction> transactions, List<SettlementRecord> settlements) {
        BigDecimal net = transactions.stream().map(SplitTransaction::signedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new MonthTotals(month, net, BalanceCalculator.fold(transactions, settlements));
    }
}
