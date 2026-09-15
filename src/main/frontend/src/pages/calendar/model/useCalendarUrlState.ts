import { useCallback, useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import { addDays, addMonths, isValidIso, type CalendarView } from '../lib/calendarWindow'

const VIEWS: CalendarView[] = ['month', 'week', 'day']

export interface CalendarUrlState {
  view: CalendarView
  /** ISO date anchoring the visible period. */
  date: string
  setView: (view: CalendarView) => void
  setDate: (date: string) => void
  goToToday: () => void
  shiftPeriod: (direction: -1 | 1) => void
}

/**
 * Keeps the visible period in the query string, so a refresh, a shared link and the browser's
 * back button all land where the reader expects.
 *
 * Both parameters are validated rather than trusted: a hand-edited URL is the one input nobody
 * controls, and `?date=2026-02-30` must show today rather than crash the grid.
 */
export function useCalendarUrlState(today: string): CalendarUrlState {
  const [searchParams, setSearchParams] = useSearchParams()

  const rawView = searchParams.get('view')
  const view = VIEWS.includes(rawView as CalendarView) ? (rawView as CalendarView) : 'month'

  const rawDate = searchParams.get('date')
  const date = rawDate && isValidIso(rawDate) ? rawDate : today

  const write = useCallback((next: { view?: CalendarView; date?: string }) => {
    setSearchParams((current) => {
      const params = new URLSearchParams(current)
      params.set('view', next.view ?? view)
      params.set('date', next.date ?? date)
      return params
    }, { replace: true })
  }, [setSearchParams, view, date])

  const setView = useCallback((next: CalendarView) => write({ view: next }), [write])
  const setDate = useCallback((next: string) => write({ date: next }), [write])
  const goToToday = useCallback(() => write({ date: today }), [write, today])

  const shiftPeriod = useCallback((direction: -1 | 1) => {
    if (view === 'month') write({ date: addMonths(date, direction) })
    else write({ date: addDays(date, direction * (view === 'week' ? 7 : 1)) })
  }, [write, view, date])

  return useMemo(
    () => ({ view, date, setView, setDate, goToToday, shiftPeriod }),
    [view, date, setView, setDate, goToToday, shiftPeriod])
}
