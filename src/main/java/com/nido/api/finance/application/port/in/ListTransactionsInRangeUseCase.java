package com.nido.api.finance.application.port.in;

import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.space.domain.model.SpaceMembership;

import java.time.LocalDate;
import java.util.List;

/**
 * The space's transactions over an arbitrary range, for callers whose window is not month-aligned.
 * {@code ListTransactionsUseCase} stays the month-based read the finance page uses.
 */
public interface ListTransactionsInRangeUseCase {
    List<Transaction> list(SpaceMembership caller, LocalDate from, LocalDate to);
}
