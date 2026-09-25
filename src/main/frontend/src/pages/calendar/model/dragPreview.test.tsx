import { describe, it, expect } from 'vitest'
import { renderHook } from '@testing-library/react'
import type { ReactNode } from 'react'
import type { CalendarOccurrence, ScheduleChange } from '@/entities/calendar'
import type { DragIntent } from '../lib/dragTypes'
import { fadeClassFor, useDragPreview } from './dragPreview'
import { DragPreviewProvider } from './DragPreviewProvider'

const meeting: CalendarOccurrence = {
  source: 'EVENT', sourceId: 'e1', seriesId: null, originalDate: null, materialized: true,
  title: 'Piano', description: null, location: null, allDay: false,
  startDate: '2026-09-23', startTime: '14:00:00', endDate: '2026-09-23', endTime: '15:30:00',
  color: null, participantIds: [],
}
const move: DragIntent = { kind: 'move', occurrence: meeting, from: 'grid', day: '2026-09-23' }
const stretch: DragIntent = { kind: 'resize-end', occurrence: meeting }
const later: ScheduleChange = { allDay: false, startDate: '2026-09-24', startTime: '09:00', endDate: '2026-09-24', endTime: '10:30' }
const home: ScheduleChange = { allDay: false, startDate: '2026-09-23', startTime: '14:00', endDate: '2026-09-23', endTime: '15:30' }

function preview(intent: DragIntent, change: ScheduleChange | null) {
  const wrapper = ({ children }: { children: ReactNode }) =>
    <DragPreviewProvider value={{ intent, change }}>{children}</DragPreviewProvider>
  return renderHook(() => useDragPreview(), { wrapper }).result.current
}

describe('useDragPreview', () => {
  it('is nothing outside a drag', () => {
    expect(renderHook(() => useDragPreview()).result.current).toBeNull()
  })

  it('draws a moved item at its landing place, its original dimmed', () => {
    const p = preview(move, later)
    expect(p?.occurrence).toMatchObject({ sourceId: 'e1', title: 'Piano', startDate: '2026-09-24', startTime: '09:00' })
    expect(fadeClassFor(p, meeting)).toBe('opacity-40')
  })

  it('stretches the item itself: the original is hidden behind the stretched copy', () => {
    const p = preview(stretch, { ...home, endTime: '17:00' })
    expect(p?.occurrence?.endTime).toBe('17:00')
    expect(fadeClassFor(p, meeting)).toBe('opacity-0')
  })

  it('shows the item as it is when it hovers its own place — no copy on top of it', () => {
    const p = preview(move, home)
    expect(p?.occurrence).toBeNull()
    expect(fadeClassFor(p, meeting)).toBe('')
  })

  it('keeps a moved item dimmed and draws no copy while the pointer is on no slot', () => {
    const p = preview(move, null)
    expect(p?.occurrence).toBeNull()
    expect(fadeClassFor(p, meeting)).toBe('opacity-40')
  })

  it('leaves every other item alone', () => {
    expect(fadeClassFor(preview(move, later), { ...meeting, sourceId: 'other' })).toBe('')
  })
})
