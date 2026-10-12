package com.nido.api.finance.domain.port.out;

import com.nido.api.finance.domain.model.Contribution;
import com.nido.api.finance.domain.model.CreateTransactionCommand;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.model.UpdateTransactionCommand;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository {
    Optional<Transaction> findById(UUID transactionId);
    /** Newest first; on the same day, the latest entry first. */
    List<Transaction> findBySpaceIdAndMonth(UUID spaceId, YearMonth month);

    /**
     * Same read over an arbitrary range. A calendar window is not aligned to months — a week can
     * straddle two, a month view straddles three — so the month-based read above cannot serve it
     * without being called several times and stitched back together.
     */
    List<Transaction> findBySpaceIdAndDateBetween(UUID spaceId, LocalDate from, LocalDate to);

    /**
     * {@code contributors} must already be resolved (equal split applied, custom shares
     * validated to sum to the amount) — that is the caller's job via
     * {@code ContributionSplitter.resolve}, never this adapter's. The adapter only persists
     * what it's given.
     */
    Transaction create(CreateTransactionCommand command, List<Contribution> contributors);
    Transaction update(UpdateTransactionCommand command, List<Contribution> contributors);
    void delete(UUID transactionId);

    /**
     * The operations of the space dated within the range, added with their sign: incomes up, expenses down. The balance
     * reads it for the part of a month up to a day.
     */
    BigDecimal sumRecordedBetween(UUID spaceId, LocalDate from, LocalDate to);
}
