package com.nido.api.finance.domain.port.out;

import com.nido.api.finance.domain.model.Contribution;
import com.nido.api.finance.domain.model.CreateTransactionCommand;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.model.UpdateTransactionCommand;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository {
    Optional<Transaction> findById(UUID transactionId);
    List<Transaction> findBySpaceIdAndMonth(UUID spaceId, YearMonth month);
    /** Every transaction ever recorded in the space — balances/settlements span all time, not just one month. */
    List<Transaction> findAllBySpaceId(UUID spaceId);
    /**
     * {@code contributors} must already be resolved (equal split applied, custom shares
     * validated to sum to the amount) — that is the caller's job via
     * {@code ContributionSplitter.resolve}, never this adapter's. The adapter only persists
     * what it's given.
     */
    Transaction create(CreateTransactionCommand command, List<Contribution> contributors);
    Transaction update(UpdateTransactionCommand command, List<Contribution> contributors);
    void delete(UUID transactionId);
}
