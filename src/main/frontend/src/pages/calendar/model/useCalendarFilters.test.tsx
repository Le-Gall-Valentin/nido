import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useCalendarFilters } from './useCalendarFilters'
import { tokenFor } from '../lib/sourceAppearance'
import type { CalendarOccurrence } from '@/entities/calendar'

function occurrence(overrides: Partial<CalendarOccurrence>): CalendarOccurrence {
  return {
    source: 'EVENT', sourceId: 'x', seriesId: null, originalDate: null, materialized: true,
    title: 'x', allDay: true, startDate: '2026-01-01', startTime: null, endDate: '2026-01-01',
    endTime: null, color: null, participantIds: [], ...overrides,
  }
}

describe('useCalendarFilters', () => {
  beforeEach(() => { localStorage.clear() })
  afterEach(() => { vi.restoreAllMocks() })

  it('enables every source the first time a space is opened', () => {
    const { result } = renderHook(() => useCalendarFilters('s1'))
    expect(result.current.enabled.size).toBe(5)
    expect(result.current.isEnabled('FINANCE')).toBe(true)
  })

  it('drops the occurrences of a disabled source', () => {
    const { result } = renderHook(() => useCalendarFilters('s1'))
    act(() => { result.current.toggle('FINANCE') })

    const kept = result.current.filter([
      occurrence({ source: 'FINANCE', title: 'Loyer' }),
      occurrence({ source: 'EVENT', title: 'Piano' }),
    ])
    expect(kept.map((o) => o.title)).toEqual(['Piano'])
  })

  it('remembers a disabled source across remounts, per space', () => {
    const first = renderHook(() => useCalendarFilters('s1'))
    act(() => { first.result.current.toggle('FINANCE') })
    first.unmount()

    const again = renderHook(() => useCalendarFilters('s1'))
    expect(again.result.current.isEnabled('FINANCE')).toBe(false)

    const otherSpace = renderHook(() => useCalendarFilters('s2'))
    expect(otherSpace.result.current.isEnabled('FINANCE')).toBe(true)
  })

  it('survives localStorage being unavailable', () => {
    // Private windows throw on access; a filter preference must not be able to break the page.
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new Error('denied') })
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('denied') })

    const { result } = renderHook(() => useCalendarFilters('s1'))
    expect(result.current.enabled.size).toBe(5)
    expect(() => act(() => { result.current.toggle('MEAL') })).not.toThrow()
    expect(result.current.isEnabled('MEAL')).toBe(false)
  })

  it('ignores stored values that are not sources', () => {
    localStorage.setItem('nido.calendar.filters.s1', JSON.stringify(['NOT_A_SOURCE', 'MEAL']))
    const { result } = renderHook(() => useCalendarFilters('s1'))
    expect(result.current.isEnabled('MEAL')).toBe(false)
    expect(result.current.enabled.size).toBe(4)
  })

  it('ignores a stored value that is not even an array', () => {
    localStorage.setItem('nido.calendar.filters.s1', '"broken"')
    const { result } = renderHook(() => useCalendarFilters('s1'))
    expect(result.current.enabled.size).toBe(5)
  })
})

describe('tokenFor', () => {
  it('falls back to the source colour when an occurrence has no override', () => {
    expect(tokenFor(occurrence({ source: 'TASK', color: null }))).toBe('status-blue')
    expect(tokenFor(occurrence({ source: 'MEAL', color: null }))).toBe('status-green')
  })

  it('honours a valid override', () => {
    expect(tokenFor(occurrence({ source: 'EVENT', color: 'status-orange' }))).toBe('status-orange')
  })

  it('ignores an override that is not one of the theme tokens', () => {
    // Stored text; a stale or hand-written value must not produce an unstyled element.
    expect(tokenFor(occurrence({ source: 'EVENT', color: '#ff00ff' }))).toBe('accent')
  })
})
