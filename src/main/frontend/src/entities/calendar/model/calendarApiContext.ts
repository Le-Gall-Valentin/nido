import { createContext, useContext } from 'react'
import type { ICalendarApi } from './ICalendarApi'
import type { IRecurringEventSeriesApi } from './IRecurringEventSeriesApi'
import type { ICalendarSyncApi } from './ICalendarSyncApi'

export type CalendarApi = ICalendarApi & IRecurringEventSeriesApi & ICalendarSyncApi

export const CalendarApiContext = createContext<CalendarApi | null>(null)

function useCalendarApiContext(): CalendarApi {
  const api = useContext(CalendarApiContext)
  if (!api) {
    throw new Error('useCalendarApi must be used within a CalendarApiProvider')
  }
  return api
}

/** Narrows the injected api to the feed and event operations — ISP, as in entities/tasks. */
export function useCalendarApi(): ICalendarApi {
  return useCalendarApiContext()
}

/** Narrows the injected api to recurring series management only. */
export function useRecurringEventSeriesApi(): IRecurringEventSeriesApi {
  return useCalendarApiContext()
}

/** Narrows the injected api to what goes in and out of the calendar. */
export function useCalendarSyncApi(): ICalendarSyncApi {
  return useCalendarApiContext()
}
