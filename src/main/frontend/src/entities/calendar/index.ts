export type {
  CalendarOccurrence, CalendarSourceType, CalendarEvent, RecurringEventSeries,
  RecurrenceInterval, Weekday, DayRule, EventInput, RecurringEventSeriesInput, ScheduleChange,
  ExportableSource, SubscriptionLink, SubscriptionLinkInput, CreatedSubscriptionLink,
  ImportNoteKind, ImportNote, ImportReport, ExternalFeed, ExternalFeedInput, ExternalFeedUpdate,
} from './model/types'
export { MAX_IMPORT_NOTES } from './model/types'
export { toEventInput } from './lib/toEventInput'
export {
  EVENT_COLORS, DEFAULT_EVENT_COLOR, FEED_FALLBACK_COLOR, EVENT_DOT_CLASS, EVENT_TINT_CLASS, isEventColor, feedColor,
  eventDotClass, eventTintClass, type EventColor,
} from './lib/eventPalette'
export type { ICalendarApi } from './model/ICalendarApi'
export type { IRecurringEventSeriesApi } from './model/IRecurringEventSeriesApi'
export type { ICalendarSyncApi } from './model/ICalendarSyncApi'
export type { CalendarApi } from './model/calendarApiContext'
export { CalendarApiProvider } from './model/CalendarApiProvider'
export { useCalendarApi, useRecurringEventSeriesApi, useCalendarSyncApi } from './model/calendarApiContext'
export {
  calendarKey, occurrencesKey, useOccurrences, recurringEventSeriesKey, useRecurringEventSeries,
} from './model/useCalendarQueries'
export {
  useCreateEvent, useUpdateEvent, useDeleteEvent, useJoinEvent, useLeaveEvent,
  useCopyEvent, useMoveEvent, useCreateRecurringEventSeries, useUpdateRecurringEventSeries,
  useDeleteRecurringEventSeries, useDetachOccurrence, useExcludeOccurrence, useCopyOccurrence, useMoveOccurrence,
} from './model/useCalendarMutations'
export { calendarApi } from './api/calendarApi'
export {
  subscriptionLinksKey, externalFeedsKey, useSubscriptionLinks, useCreateSubscriptionLink, useRevokeSubscriptionLink,
  useExternalFeeds, useFollowExternalFeed, useUpdateExternalFeed, useRemoveExternalFeed, useRefreshExternalFeed,
  useAnalyseImport, useImportFile, useExportCalendar,
} from './model/useCalendarSync'
export { CalendarSyncError, toSyncError, SYNC_REASONS, type SyncReason } from './api/calendarSyncErrors'
export { IntervalTooLargeError } from './api/seriesErrors'
