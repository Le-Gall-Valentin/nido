import { createContext, useContext, type ReactNode } from 'react'
import type { ICalendarApi } from './ICalendarApi'
import type { IRecurringEventSeriesApi } from './IRecurringEventSeriesApi'

export type CalendarApi = ICalendarApi & IRecurringEventSeriesApi

const CalendarApiContext = createContext<CalendarApi | null>(null)

interface CalendarApiProviderProps {
  api: CalendarApi
  children: ReactNode
}

/** Injects the ICalendarApi/IRecurringEventSeriesApi implementation the calendar's hooks consume. */
export function CalendarApiProvider({ api, children }: CalendarApiProviderProps) {
  return <CalendarApiContext.Provider value={api}>{children}</CalendarApiContext.Provider>
}

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
