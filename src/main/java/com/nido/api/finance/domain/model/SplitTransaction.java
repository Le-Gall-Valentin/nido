package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * An operation reduced to what a balance is made of — the space's and its members'. The space's balance moves by its
 * {@link #signedAmount()}. A member's moves when it is shared: one member handled an amount, and named members each
 * hold a share of it.
 *
 * <p>Which way that moves a member's balance depends on the type, and the two are mirror images. On an EXPENSE the
 * member fronted the money and the others owe their share back. On an INCOME the member received money partly belonging
 * to the others, so the debt runs the other way — hence {@code type}, without which a shared refund would be folded as
 * though it were a shared bill.
 *
 * <p>Neither {@link BalanceCalculator} nor the space's net looks at anything else — not the label, the category, the
 * date or the recurring series. Reading the full {@link Transaction} to fold them would decrypt every label for nothing.
 *
 * <p>Folded month by month into {@link MonthTotals}, which finance_month_totals and finance_month_member_totals keep: a
 * balance is then read from a dozen figures a year instead of every operation ever recorded, which twenty years of a
 * household would make slow. Those tables are encrypted aggregates, which this record once ruled out for the drift they
 * risk. {@code MonthTotalsWriter} answers that risk: the operations stay the only source of truth, a month is folded
 * again whole at every write that touches it — never adjusted by a difference — and a version rebuilds every space from
 * the operations when in doubt.
 */
public record SplitTransaction(
    /** The member who paid on an EXPENSE, the one who received on an INCOME; null when nobody handled it for the others. */
    UUID payerId,
    BigDecimal amount,
    /** Who owes a share on an EXPENSE, who is owed one on an INCOME; empty when nobody shares it. */
    List<Contribution> contributors,
    TransactionType type
) {
    /** What it moves the space's balance by: up for an income, down for an expense. */
    public BigDecimal signedAmount() {
        return type.signed(amount);
    }
}
