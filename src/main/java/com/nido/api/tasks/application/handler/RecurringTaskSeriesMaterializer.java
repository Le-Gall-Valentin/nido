package com.nido.api.tasks.application.handler;

import com.nido.api.tasks.domain.model.CreateTaskCommand;
import com.nido.api.tasks.domain.model.RecurrenceScheduler;
import com.nido.api.tasks.domain.model.RecurringTaskSeries;
import com.nido.api.tasks.domain.model.SubtaskInput;
import com.nido.api.tasks.domain.port.out.RecurringTaskSeriesRepository;
import com.nido.api.tasks.domain.port.out.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lazily materializes any recurring task occurrence whose lead-time window has
 * opened — shared by the tasks module's only read path ({@code ListTasksHandler})
 * so a series never silently misses an occurrence. Unlike finance's equivalent
 * materializer, "due" here means the occurrence's due date minus its lead time is
 * on or before today, not the due date itself — that's the whole point of the
 * lead-time feature: a task can appear before its actual deadline.
 *
 * <p>Every due occurrence for a series is computed in memory first and persisted
 * with a single {@link TaskRepository#createAll} call, not one write per
 * occurrence: after a long absence a series can have many overdue occurrences to
 * catch up on at once, and one batched write is both far faster than N round
 * trips and avoids the failure mode where a timeout partway through N sequential
 * writes rolls back every occurrence — since {@code advance} is only ever called
 * once the whole batch already succeeded, a retry never repeats already-persisted
 * work.
 *
 * <p>That batch is capped at {@link #MAX_OCCURRENCES_PER_RUN} occurrences per series.
 * Without the cap the loop below runs once per missed occurrence with no ceiling, so a
 * series anchored far enough in the past fills the pending list until the heap gives out —
 * before a single row is written. {@code RecurrenceScheduler.validateBacklog} refuses such
 * a series at creation; this cap is what protects series that predate that rule, or that
 * fell far behind while nobody opened the space.
 */
final class RecurringTaskSeriesMaterializer {

    private static final Logger log = LoggerFactory.getLogger(RecurringTaskSeriesMaterializer.class);

    /**
     * Occurrences a single pass may materialize per series. Generous enough that any
     * realistic catch-up completes in one pass, low enough that a pathological series
     * costs a bounded amount of memory and a bounded batch insert per request.
     */
    static final int MAX_OCCURRENCES_PER_RUN = 500;

    private RecurringTaskSeriesMaterializer() {}

    static void materializeDueOccurrences(
            TaskRepository taskRepository, RecurringTaskSeriesRepository seriesRepository, UUID spaceId, LocalDate today) {
        seriesRepository.lockForMaterialization(spaceId);
        for (RecurringTaskSeries series : seriesRepository.findBySpaceId(spaceId)) {
            int occurrence = series.occurrenceCount() + 1;
            int rotationIndex = series.currentRotationIndex();
            int lastGenerated = series.occurrenceCount();
            List<CreateTaskCommand> due = new ArrayList<>();
            boolean capped = false;
            while (true) {
                if (due.size() >= MAX_OCCURRENCES_PER_RUN) {
                    capped = true;
                    break;
                }
                LocalDate dueDate = RecurrenceScheduler.nextDueDate(
                    series.anchorDate(), series.intervalType(), series.intervalCount(), occurrence);
                if (series.endDate() != null && dueDate.isAfter(series.endDate())) {
                    break;
                }
                LocalDate windowOpensOn = RecurrenceScheduler.minus(dueDate, series.leadIntervalType(), series.leadIntervalCount());
                if (windowOpensOn.isAfter(today)) {
                    break;
                }
                List<UUID> assignees = List.of();
                if (!series.rotationMemberIds().isEmpty()) {
                    rotationIndex = (rotationIndex + 1) % series.rotationMemberIds().size();
                    assignees = List.of(series.rotationMemberIds().get(rotationIndex));
                }
                List<SubtaskInput> subtasks = series.subtaskTemplates().stream()
                    .map(text -> new SubtaskInput(text, false)).toList();
                due.add(new CreateTaskCommand(spaceId, series.title(), series.priority(), dueDate, assignees, subtasks, series.id(), series.createdBy()));
                lastGenerated = occurrence;
                occurrence++;
            }
            if (!due.isEmpty()) {
                taskRepository.createAll(due);
                seriesRepository.advance(series.id(), rotationIndex, lastGenerated);
                if (capped) {
                    // Not an error: advance() recorded the last occurrence generated along with
                    // the rotation position, so the next read picks up exactly where this stopped.
                    log.warn("Recurring series {} hit the {}-occurrence materialization ceiling in space {}; "
                            + "caught up to occurrence {}, remainder deferred to the next read",
                        series.id(), MAX_OCCURRENCES_PER_RUN, spaceId, lastGenerated);
                }
            }
        }
    }
}
