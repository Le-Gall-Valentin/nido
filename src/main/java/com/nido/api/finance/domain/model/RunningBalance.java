package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * The balance of a space at the end of any day, from the net of each month — income minus expenses — and, when one is
 * set, the opening balance that anchors it. Without one, the balance is zero before the first operation.
 *
 * <p>With S(d) the sum of every operation dated up to the day d, the balance at d is S(d) without an opening balance,
 * and {@code amount + S(d) − S(asOf)} with one — before the anchor as well as after it. Every month before d is summed
 * whole from its net; the month d falls in is summed up to d by the caller, who has its operations at hand.
 *
 * <p>Pure: it is handed the month nets and never reads them, so a balance costs a dozen figures a year whatever the
 * number of operations behind them.
 */
public final class RunningBalance {

    private final NavigableMap<YearMonth, BigDecimal> netByMonth;
    private final BigDecimal offset;

    private RunningBalance(NavigableMap<YearMonth, BigDecimal> netByMonth, BigDecimal offset) {
        this.netByMonth = netByMonth;
        this.offset = offset;
    }

    public static RunningBalance of(Map<YearMonth, BigDecimal> netByMonth) {
        return new RunningBalance(new TreeMap<>(netByMonth), BigDecimal.ZERO);
    }

    /**
     * The same balance, anchored on an opening balance.
     *
     * @param recordedInItsMonthThroughItsDay the signed sum of the operations of the opening balance's month dated up to
     *                                        its day
     */
    public RunningBalance anchoredAt(OpeningBalance opening, BigDecimal recordedInItsMonthThroughItsDay) {
        BigDecimal recordedThroughAnchor = sumBefore(YearMonth.from(opening.asOf())).add(recordedInItsMonthThroughItsDay);
        return new RunningBalance(netByMonth, opening.amount().subtract(recordedThroughAnchor));
    }

    /** The balance at the end of the month's last day. */
    public BigDecimal atEndOf(YearMonth month) {
        return sum(netByMonth.headMap(month, true)).add(offset);
    }

    /**
     * The balance at the end of a day.
     *
     * @param recordedInItsMonthThroughIt the signed sum of the operations of the day's month dated up to that day
     */
    public BigDecimal at(LocalDate day, BigDecimal recordedInItsMonthThroughIt) {
        return sumBefore(YearMonth.from(day)).add(recordedInItsMonthThroughIt).add(offset);
    }

    private BigDecimal sumBefore(YearMonth month) {
        return sum(netByMonth.headMap(month, false));
    }

    private static BigDecimal sum(Map<YearMonth, BigDecimal> nets) {
        return nets.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
