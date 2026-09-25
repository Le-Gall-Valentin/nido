package com.nido.api.finance.application.port.in;

import com.nido.api.finance.domain.model.ProjectedOccurrence;
import com.nido.api.space.domain.model.SpaceMembership;

import java.time.LocalDate;
import java.util.List;

/**
 * Future occurrences of the space's recurring series over an arbitrary range.
 *
 * <p>Separate from {@code GetProjectionUseCase}, which answers a different question — "what will
 * this month's balance be?" — and does more than project: it materializes what is due and sums
 * the actual balance. This one only lists dates, for a calendar window that is not month-aligned.
 * Both go through {@code RecurrenceProjector.occurrencesBetween}, so the arithmetic exists once.
 */
public interface ProjectRecurringSeriesUseCase {
    List<ProjectedOccurrence> project(SpaceMembership caller, LocalDate from, LocalDate to);
}
