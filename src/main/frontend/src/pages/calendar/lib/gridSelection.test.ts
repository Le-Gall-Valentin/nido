import { describe, it, expect } from 'vitest'
import { bandRange, selectionRange } from './gridSelection'

const at = (day: string, hhmm: string) => {
  const [h, m] = hhmm.split(':').map(Number)
  return { day, minutes: h * 60 + m }
}

describe('selectionRange — a time picked out in the hour grid', () => {
  it('gives a one-hour event from the quarter hour clicked', () => {
    expect(selectionRange(at('2026-09-23', '14:20'), at('2026-09-23', '14:20'))).toEqual({
      allDay: false, startDate: '2026-09-23', startTime: '14:15', endDate: '2026-09-23', endTime: '15:15',
    })
  })

  it('counts a wobble inside the clicked quarter hour as a click', () => {
    expect(selectionRange(at('2026-09-23', '14:16'), at('2026-09-23', '14:29'))).toMatchObject({ startTime: '14:15', endTime: '15:15' })
  })

  it('covers every quarter hour the drag touched, from the first to the last', () => {
    expect(selectionRange(at('2026-09-23', '10:05'), at('2026-09-23', '11:40'))).toEqual({
      allDay: false, startDate: '2026-09-23', startTime: '10:00', endDate: '2026-09-23', endTime: '11:45',
    })
  })

  it('reads a drag upwards the same way', () => {
    expect(selectionRange(at('2026-09-23', '11:40'), at('2026-09-23', '10:05'))).toMatchObject({ startTime: '10:00', endTime: '11:45' })
  })

  it('spans several days when the drag crosses columns', () => {
    expect(selectionRange(at('2026-09-23', '14:00'), at('2026-09-25', '09:50'))).toEqual({
      allDay: false, startDate: '2026-09-23', startTime: '14:00', endDate: '2026-09-25', endTime: '10:00',
    })
  })

  it('ends at midnight as the next day at 00:00, as the form and the API expect', () => {
    expect(selectionRange(at('2026-09-23', '22:00'), at('2026-09-23', '23:59'))).toMatchObject({
      startTime: '22:00', endDate: '2026-09-24', endTime: '00:00',
    })
    expect(selectionRange(at('2026-09-23', '23:40'), at('2026-09-23', '23:40'))).toMatchObject({
      startDate: '2026-09-23', startTime: '23:30', endDate: '2026-09-24', endTime: '00:30',
    })
  })
})

describe('bandRange — days picked out in the all-day band', () => {
  it('gives an all-day event over the days touched, in either direction', () => {
    const range = { allDay: true, startDate: '2026-09-21', startTime: null, endDate: '2026-09-23', endTime: null }
    expect(bandRange('2026-09-21', '2026-09-23')).toEqual(range)
    expect(bandRange('2026-09-23', '2026-09-21')).toEqual(range)
  })
})
