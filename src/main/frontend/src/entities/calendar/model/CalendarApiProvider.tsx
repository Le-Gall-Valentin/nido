import type { ReactNode } from 'react'
import { CalendarApiContext, type CalendarApi } from './calendarApiContext'

interface CalendarApiProviderProps {
  api: CalendarApi
  children: ReactNode
}

/** Injects the ICalendarApi/IRecurringEventSeriesApi implementation the calendar's hooks consume. */
export function CalendarApiProvider({ api, children }: CalendarApiProviderProps) {
  return <CalendarApiContext.Provider value={api}>{children}</CalendarApiContext.Provider>
}
