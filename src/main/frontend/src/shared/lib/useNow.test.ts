import { describe, it, expect, vi, afterEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import { nowInZone, useNow } from './useNow'

describe('nowInZone', () => {
  it('reads date and time on the space calendar, not the machine one', () => {
    const instant = new Date('2026-09-26T22:30:00Z')
    expect(nowInZone(instant, 'Europe/Paris')).toEqual({ date: '2026-09-27', time: '00:30' })
    expect(nowInZone(instant, 'America/Toronto')).toEqual({ date: '2026-09-26', time: '18:30' })
  })
})

describe('useNow', () => {
  afterEach(() => { vi.useRealTimers() })

  it('turns the minute when the clock does, not a minute after the page opened', () => {
    // Opened 40 seconds into 10:15: counting sixty seconds from there showed 10:15 until 10:16:40.
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-09-28T10:15:40Z'))
    const { result } = renderHook(() => useNow('UTC'))
    expect(result.current.time).toBe('10:15')

    act(() => { vi.advanceTimersByTime(19_999) })
    expect(result.current.time).toBe('10:15')
    act(() => { vi.advanceTimersByTime(1) })
    expect(result.current.time).toBe('10:16')
  })

  it('keeps turning on every minute after the first', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-09-28T10:15:40Z'))
    const { result } = renderHook(() => useNow('UTC'))

    act(() => { vi.advanceTimersByTime(20_000 + 60_000) })
    expect(result.current.time).toBe('10:17')
    act(() => { vi.advanceTimersByTime(60_000) })
    expect(result.current.time).toBe('10:18')
  })
})
