import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { useCalendarFilters } from './useCalendarFilters'
import type { CalendarOccurrence } from '@/entities/calendar'

function occurrence(overrides: Partial<CalendarOccurrence>): CalendarOccurrence {
  return {
    source: 'EVENT', sourceId: 'x', seriesId: null, originalDate: null, materialized: true,
    title: 'x', description: null, location: null, allDay: true, startDate: '2026-01-01', startTime: null, endDate: '2026-01-01',
    endTime: null, color: null, participantIds: [], externalFeedId: null, ...overrides,
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

  it('shows the entries of a followed calendar, whichever sources are hidden', () => {
    const { result } = renderHook(() => useCalendarFilters('s1'))
    act(() => { result.current.toggle('EVENT') })
    const kept = result.current.filter([
      occurrence({ source: 'EXTERNAL', title: 'Ecole', externalFeedId: 'f1' }),
      occurrence({ source: 'EVENT', title: 'Piano' }),
    ])
    expect(kept.map((o) => o.title)).toEqual(['Ecole'])
  })

  it('hides one followed calendar without hiding the others, and remembers it', () => {
    const { result } = renderHook(() => useCalendarFilters('s1'))
    act(() => result.current.toggleFeed('f1'))

    expect(result.current.isFeedEnabled('f1')).toBe(false)
    expect(result.current.isFeedEnabled('f2')).toBe(true)
    expect(result.current.filter([
      occurrence({ source: 'EXTERNAL', externalFeedId: 'f1' }),
      occurrence({ source: 'EXTERNAL', externalFeedId: 'f2' }),
      occurrence({ source: 'EVENT' }),
    ]).map((o) => o.externalFeedId)).toEqual(['f2', null])
    expect(renderHook(() => useCalendarFilters('s1')).result.current.isFeedEnabled('f1')).toBe(false)
    expect(renderHook(() => useCalendarFilters('s2')).result.current.isFeedEnabled('f1')).toBe(true)
  })

  it('shows a followed calendar again once toggled back, and ignores a stored value that is no list of ids', () => {
    localStorage.setItem('nido.calendar.feeds.s1', JSON.stringify(['f1', 7, null]))
    const { result } = renderHook(() => useCalendarFilters('s1'))
    expect(result.current.isFeedEnabled('f1')).toBe(false)
    act(() => result.current.toggleFeed('f1'))
    expect(result.current.isFeedEnabled('f1')).toBe(true)

    localStorage.setItem('nido.calendar.feeds.s3', '"broken"')
    expect(renderHook(() => useCalendarFilters('s3')).result.current.isFeedEnabled('f1')).toBe(true)
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
