import { describe, it, expect, vi, afterEach } from 'vitest'
import { useEffect, useState } from 'react'
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

  it('reads the date of a zone in the very render it arrives in', () => {
    // The zone arrives after the first render, with the spaces list. The effects of that render — a
    // link opening the new-event form on "today" — read the previous zone's date, which opened the
    // form on the machine's day in the evening in America. Two zones 25 hours apart are always on
    // different days, whatever the machine's own zone.
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-09-29T23:30:00Z'))
    const seen: string[] = []
    let setZone: (zone: string) => void = () => {}
    renderHook(() => {
      const [zone, set] = useState('Pacific/Pago_Pago')
      setZone = set
      const { date } = useNow(zone)
      useEffect(() => { seen.push(`${zone} ${date}`) }, [zone, date])
    })

    act(() => { setZone('Pacific/Kiritimati') })
    expect(seen).toEqual(['Pacific/Pago_Pago 2026-09-29', 'Pacific/Kiritimati 2026-09-30'])
  })
})
