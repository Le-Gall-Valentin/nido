import { describe, it, expect } from 'vitest'
import type { AgendaEvent } from '@/entities/dashboard'
import { isEventOver, nowLineIndex } from './nowLine'

function event(id: string, startTime: string, endTime: string | null, endDate = '2026-09-26'): AgendaEvent {
  return { id, title: id, location: null, color: null, startDate: '2026-09-26', endDate, startTime, endTime, participantIds: [] }
}

const DAY = [event('market', '10:30', '12:00'), event('doctor', '16:00', '16:30'), event('dinner', '19:30:00', null)]

describe('now line', () => {
  it('sits after every event that has started', () => {
    expect(nowLineIndex(DAY, '09:00')).toBe(0)
    expect(nowLineIndex(DAY, '14:32')).toBe(1)
    expect(nowLineIndex(DAY, '16:10')).toBe(2)
    expect(nowLineIndex(DAY, '23:00')).toBe(3)
  })

  it('dims an event once its end — or its start, without an end — has passed', () => {
    expect(isEventOver(DAY[0], '2026-09-26', '14:32')).toBe(true)
    expect(isEventOver(DAY[1], '2026-09-26', '16:10')).toBe(false)
    expect(isEventOver(DAY[2], '2026-09-26', '19:30')).toBe(true)
  })

  it('never dims an event that goes on into a later day', () => {
    expect(isEventOver(event('night shift', '22:00', '02:00', '2026-09-27'), '2026-09-26', '23:30')).toBe(false)
  })
})
