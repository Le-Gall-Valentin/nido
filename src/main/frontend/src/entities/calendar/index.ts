export type {
  CalendarOccurrence, CalendarSourceType, CalendarEvent, RecurringEventSeries,
  RecurrenceInterval, EventInput, RecurringEventSeriesInput, ScheduleChange,
} from './model/types'
export { toEventInput } from './lib/toEventInput'
export {
  EVENT_COLORS, DEFAULT_EVENT_COLOR, EVENT_DOT_CLASS, EVENT_TINT_CLASS, isEventColor, eventDotClass, eventTintClass,
  type EventColor,
} from './lib/eventPalette'
export type { ICalendarApi } from './model/ICalendarApi'
export type { IRecurringEventSeriesApi } from './model/IRecurringEventSeriesApi'
export type { CalendarApi } from './model/calendarApiContext'
export { CalendarApiProvider, useCalendarApi, useRecurringEventSeriesApi } from './model/calendarApiContext'
export {
  calendarKey, occurrencesKey, useOccurrences, recurringEventSeriesKey, useRecurringEventSeries,
} from './model/useCalendarQueries'
export {
  useCreateEvent, useUpdateEvent, useDeleteEvent, useJoinEvent, useLeaveEvent,
  useCopyEvent, useMoveEvent, useCreateRecurringEventSeries, useUpdateRecurringEventSeries,
  useDeleteRecurringEventSeries, useDetachOccurrence, useExcludeOccurrence, useCopyOccurrence, useMoveOccurrence,
} from './model/useCalendarMutations'
export { calendarApi } from './api/calendarApi'
