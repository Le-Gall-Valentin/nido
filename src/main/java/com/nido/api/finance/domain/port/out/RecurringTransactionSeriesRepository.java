package com.nido.api.finance.domain.port.out;

import com.nido.api.finance.domain.model.Contribution;
import com.nido.api.finance.domain.model.CreateRecurringSeriesCommand;
import com.nido.api.finance.domain.model.RecurringTransactionSeries;
import com.nido.api.finance.domain.model.UpdateRecurringSeriesCommand;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecurringTransactionSeriesRepository {
    Optional<RecurringTransactionSeries> findById(UUID seriesId);
    List<RecurringTransactionSeries> findBySpaceId(UUID spaceId);
    /** Series whose {@code endDate} is null or on/after {@code asOf} — candidates for lazy materialization or projection. */
    List<RecurringTransactionSeries> findActiveBySpaceId(UUID spaceId, LocalDate asOf);
    /**
     * {@code contributors} must already be resolved (equal split applied, custom shares
     * validated to sum to the amount) — that is the caller's job via
     * {@code ContributionSplitter.resolve}, never this adapter's. The adapter only persists
     * what it's given.
     */
    RecurringTransactionSeries create(CreateRecurringSeriesCommand command, List<Contribution> contributors);
    RecurringTransactionSeries update(UpdateRecurringSeriesCommand command, List<Contribution> contributors);
    void delete(UUID seriesId);
    RecurringTransactionSeries advanceLastMaterializedDate(UUID seriesId, LocalDate newDate);
}
