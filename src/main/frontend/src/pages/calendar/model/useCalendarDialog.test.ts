import { describe, it, expect } from 'vitest'
import { act, renderHook } from '@testing-library/react'
import type { CalendarOccurrence } from '@/entities/calendar'
import { useCalendarDialog } from './useCalendarDialog'

const dentist: CalendarOccurrence = {
  source: 'EVENT', sourceId: 'e1', seriesId: null, originalDate: null, materialized: true,
  title: 'Dentiste', description: null, location: null, allDay: true, startDate: '2026-10-06', startTime: null,
  endDate: '2026-10-06', endTime: null, color: null, participantIds: [],
}
const piano: CalendarOccurrence = {
  ...dentist, sourceId: 's1:2026-10-13', seriesId: 's1', originalDate: '2026-10-13', materialized: false, title: 'Piano',
}

function dialog() {
  return renderHook(() => useCalendarDialog()).result
}

describe('useCalendarDialog', () => {
  it('opens one dialog at a time, the next replacing the one before', () => {
    const result = dialog()
    act(() => result.current.show({ kind: 'day', date: '2026-10-06' }))
    act(() => result.current.show({ kind: 'occurrence', sourceId: 'e1' }))
    expect(result.current.open).toEqual({ kind: 'occurrence', sourceId: 'e1' })
    act(() => result.current.close())
    expect(result.current.open).toBeNull()
  })

  it('edits or deletes a single event at once', () => {
    const result = dialog()
    act(() => result.current.act(dentist, 'edit'))
    expect(result.current.open).toEqual({ kind: 'edit', occurrence: dentist, detachSlot: null })
    act(() => result.current.act(dentist, 'delete'))
    expect(result.current.open).toEqual({ kind: 'delete', occurrence: dentist })
  })

  it('asks first about an occurrence of a series', () => {
    const result = dialog()
    act(() => result.current.act(piano, 'edit'))
    expect(result.current.open).toEqual({ kind: 'scope', occurrence: piano, action: 'edit' })
  })

  it('edits only the occurrence on its own slot when that is the answer', () => {
    const result = dialog()
    act(() => result.current.act(piano, 'edit'))
    act(() => result.current.answerScope('occurrence'))
    expect(result.current.open).toEqual({ kind: 'edit', occurrence: piano, detachSlot: { seriesId: 's1', date: '2026-10-13' } })
  })

  it('deletes only the occurrence when that is the answer', () => {
    const result = dialog()
    act(() => result.current.act(piano, 'delete'))
    act(() => result.current.answerScope('occurrence'))
    expect(result.current.open).toEqual({ kind: 'delete', occurrence: piano })
  })

  it('acts on the whole series when that is the answer', () => {
    const result = dialog()
    act(() => result.current.act(piano, 'delete'))
    act(() => result.current.answerScope('series'))
    expect(result.current.open).toEqual({ kind: 'series', seriesId: 's1', action: 'delete' })
  })

  it('ignores an answer to a question nobody asked', () => {
    const result = dialog()
    act(() => result.current.show({ kind: 'day', date: '2026-10-06' }))
    act(() => result.current.answerScope('series'))
    expect(result.current.open).toEqual({ kind: 'day', date: '2026-10-06' })
  })

  it('hands back to the day a new event was added from once its form closes, and to nothing otherwise', () => {
    const result = dialog()
    act(() => result.current.show({ kind: 'create', date: '2026-10-06', returnToDay: true }))
    act(() => result.current.closeForm())
    expect(result.current.open).toEqual({ kind: 'day', date: '2026-10-06' })

    act(() => result.current.show({ kind: 'create', date: '2026-10-06', returnToDay: false }))
    act(() => result.current.closeForm())
    expect(result.current.open).toBeNull()

    act(() => result.current.act(dentist, 'edit'))
    act(() => result.current.closeForm())
    expect(result.current.open).toBeNull()
  })
})
