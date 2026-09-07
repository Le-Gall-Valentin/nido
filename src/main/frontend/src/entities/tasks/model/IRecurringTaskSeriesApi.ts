import type { RecurrenceInput, RecurringTaskSeries, TaskPriority } from './types'

/**
 * Port for recurring task series management (list/edit/delete the series
 * themselves, as opposed to the materialized task instances they produce).
 * Split out from ITasksApi so consumers that only manage series don't
 * depend on the unrelated task-mutation surface.
 */
export interface IRecurringTaskSeriesApi {
  listRecurringTaskSeries(spaceId: string): Promise<RecurringTaskSeries[]>
  updateRecurringTaskSeries(
    spaceId: string, seriesId: string, title: string, priority: TaskPriority, subtasks: string[], recurrence: RecurrenceInput
  ): Promise<RecurringTaskSeries>
  deleteRecurringTaskSeries(spaceId: string, seriesId: string): Promise<void>
}
