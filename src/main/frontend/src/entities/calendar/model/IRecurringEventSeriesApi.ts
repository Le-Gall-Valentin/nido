import type { CalendarEvent, EventInput, RecurringEventSeries, RecurringEventSeriesInput } from './types'

export interface IRecurringEventSeriesApi {
  listRecurringEventSeries(spaceId: string): Promise<RecurringEventSeries[]>
  createRecurringEventSeries(spaceId: string, input: RecurringEventSeriesInput): Promise<RecurringEventSeries>
  updateRecurringEventSeries(spaceId: string, seriesId: string, input: RecurringEventSeriesInput): Promise<RecurringEventSeries>
  deleteRecurringEventSeries(spaceId: string, seriesId: string): Promise<void>
  /** Idempotent upsert of one (series, slot) pair — replaying it yields the same state. */
  detachOccurrence(spaceId: string, seriesId: string, date: string, input: EventInput): Promise<CalendarEvent>
  excludeOccurrence(spaceId: string, seriesId: string, date: string): Promise<void>
}
