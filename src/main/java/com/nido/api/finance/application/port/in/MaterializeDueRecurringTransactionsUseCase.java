package com.nido.api.finance.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;

/**
 * Turns the recurring operations that have fallen due into real transactions, exactly as opening the
 * finance page does — same algorithm, same advisory lock.
 *
 * <p>A write. Call it in a transaction of its own, never from inside a read-only one.
 */
public interface MaterializeDueRecurringTransactionsUseCase {
    void materialize(SpaceMembership caller);
}
