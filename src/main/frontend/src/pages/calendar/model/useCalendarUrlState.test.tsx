import { describe, it, expect } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { MemoryRouter, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useCalendarUrlState } from './useCalendarUrlState'

const TODAY = '2026-09-14'

function wrapperFor(initialEntry: string) {
  return ({ children }: { children: ReactNode }) => (
    <MemoryRouter initialEntries={[initialEntry]}>{children}</MemoryRouter>
  )
}

function renderState(initialEntry: string) {
  return renderHook(() => ({ state: useCalendarUrlState(TODAY), location: useLocation() }), {
    wrapper: wrapperFor(initialEntry),
  })
}

describe('useCalendarUrlState', () => {
  it('defaults to the month view anchored on the household today', () => {
    const { result } = renderState('/c')
    expect(result.current.state.view).toBe('month')
    expect(result.current.state.date).toBe(TODAY)
  })

  it('reads a valid view and date out of the query string', () => {
    const { result } = renderState('/c?view=week&date=2026-03-02')
    expect(result.current.state.view).toBe('week')
    expect(result.current.state.date).toBe('2026-03-02')
  })

  it('falls back to the default when the view is not one of the three', () => {
    const { result } = renderState('/c?view=decade&date=2026-03-02')
    expect(result.current.state.view).toBe('month')
  })

  it('falls back to today when the date is not a real date', () => {
    // A hand-edited URL is the one input nobody controls; 2026-02-30 must not crash the grid.
    const { result } = renderState('/c?view=week&date=2026-02-30')
    expect(result.current.state.date).toBe(TODAY)
  })

  it('shifts by a month in month view, clamping rather than spilling over', () => {
    const { result } = renderState('/c?view=month&date=2026-01-31')
    act(() => { result.current.state.shiftPeriod(1) })
    expect(result.current.state.date).toBe('2026-02-28')
  })

  it('shifts by seven days in week view and one in day view', () => {
    const week = renderState('/c?view=week&date=2026-01-08')
    act(() => { week.result.current.state.shiftPeriod(1) })
    expect(week.result.current.state.date).toBe('2026-01-15')

    const day = renderState('/c?view=day&date=2026-01-08')
    act(() => { day.result.current.state.shiftPeriod(-1) })
    expect(day.result.current.state.date).toBe('2026-01-07')
  })

  it('goes back to the household today', () => {
    const { result } = renderState('/c?view=day&date=2026-01-08')
    act(() => { result.current.state.goToToday() })
    expect(result.current.state.date).toBe(TODAY)
  })

  it('writes the state back into the query string so a refresh restores it', () => {
    const { result } = renderState('/c')
    act(() => { result.current.state.setView('day') })
    expect(result.current.location.search).toContain('view=day')
    expect(result.current.location.search).toContain(`date=${TODAY}`)
  })

  it('keeps the view when only the date changes', () => {
    const { result } = renderState('/c?view=week&date=2026-01-08')
    act(() => { result.current.state.setDate('2026-02-02') })
    expect(result.current.state.view).toBe('week')
    expect(result.current.state.date).toBe('2026-02-02')
  })
})
