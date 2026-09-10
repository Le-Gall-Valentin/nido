package com.nido.api.finance.domain.port.out;

import com.nido.api.finance.domain.model.Contribution;
import com.nido.api.finance.domain.model.CreateRecurringSeriesCommand;
import com.nido.api.finance.domain.model.RecurringSeriesSchedule;
import com.nido.api.finance.domain.model.RecurringTransactionSeries;
import com.nido.api.finance.domain.model.UpdateRecurringSeriesCommand;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecurringTransactionSeriesRepository {
    Optional<RecurringTransactionSeries> findById(UUID seriesId);
    List<RecurringTransactionSeries> findBySpaceId(UUID spaceId);

    /**
     * Scheduling fields only, for deciding whether a space owes anything before committing to
     * the work of materializing it. Cheap, and safe to call before
     * {@link #lockForMaterialization(UUID)} — see {@link RecurringSeriesSchedule}.
     */
    List<RecurringSeriesSchedule> findSchedulesBySpaceId(UUID spaceId);
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

    /**
     * Serializes concurrent lazy materialization for a space: held until the caller's
     * transaction commits or rolls back, so two requests racing to materialize the same
     * due occurrence can't both read the same {@code lastMaterializedDate} and each insert
     * it — the second one blocks here until the first commits, then sees the advanced
     * cursor and has nothing left to do. See {@code RecurringTransactionMaterializer}.
     */
    void lockForMaterialization(UUID spaceId);
}
