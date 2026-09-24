import { describe, it, expect } from 'vitest'
import { isUnchanged, previewDrop, resolveDrop, sameSchedule } from './dragResolution'
import type { CalendarOccurrence } from '@/entities/calendar'

function base(overrides: Partial<CalendarOccurrence>): CalendarOccurrence {
  return {
    source: 'EVENT', sourceId: 'e1', seriesId: null, originalDate: null, materialized: true,
    title: 'x', description: null, location: null, allDay: false,
    startDate: '2026-09-23', startTime: '14:00:00', endDate: '2026-09-23', endTime: '15:30:00',
    color: null, participantIds: [], ...overrides,
  }
}
const meeting = base({})
const birthday = base({ allDay: true, startTime: null, endTime: null })
/** A move grabbed by its piece on `day` (its first day by default), at `grabMinutes` in the grid. */
const move = (occurrence: CalendarOccurrence, from: 'cell' | 'band' | 'grid' | 'row' = 'grid',
  day: string = occurrence.startDate, grabMinutes?: number) =>
  ({ kind: 'move' as const, occurrence, from, day, grabMinutes })

describe('resolveDrop — moving to another day', () => {
  it('keeps the time and the duration', () => {
    expect(resolveDrop(move(meeting, 'cell'), { kind: 'day', day: '2026-09-25' }, null)).toEqual({
      allDay: false, startDate: '2026-09-25', startTime: '14:00', endDate: '2026-09-25', endTime: '15:30',
    })
  })

  it('keeps how many days a multi-day event spans', () => {
    const trip = base({ allDay: true, startTime: null, endTime: null, startDate: '2026-09-01', endDate: '2026-09-05' })
    expect(resolveDrop(move(trip, 'cell'), { kind: 'day', day: '2026-09-10' }, null))
      .toMatchObject({ startDate: '2026-09-10', endDate: '2026-09-14' })
  })

  it('writes nothing when dropped back where it was', () => {
    expect(resolveDrop(move(meeting, 'cell'), { kind: 'day', day: '2026-09-23' }, null)).toBeNull()
  })
})

describe('resolveDrop — the hour grid', () => {
  it('starts a timed event at the pointer and keeps its duration', () => {
    expect(resolveDrop(move(meeting), { kind: 'hours', day: '2026-09-24' }, 9 * 60 + 15)).toEqual({
      allDay: false, startDate: '2026-09-24', startTime: '09:15', endDate: '2026-09-24', endTime: '10:45',
    })
  })

  it('lets a late drop run past midnight into the next day', () => {
    expect(resolveDrop(move(meeting), { kind: 'hours', day: '2026-09-24' }, 23 * 60)).toMatchObject({
      startDate: '2026-09-24', startTime: '23:00', endDate: '2026-09-25', endTime: '00:30',
    })
  })

  it('turns an all-day event dropped in the grid into a one-hour event', () => {
    expect(resolveDrop(move(birthday, 'band'), { kind: 'hours', day: '2026-09-23' }, 18 * 60)).toEqual({
      allDay: false, startDate: '2026-09-23', startTime: '18:00', endDate: '2026-09-23', endTime: '19:00',
    })
  })

  it('refuses a grid drop that has no pointer time', () => {
    expect(resolveDrop(move(meeting), { kind: 'hours', day: '2026-09-24' }, null)).toBeNull()
  })
})

describe('resolveDrop — the all-day band', () => {
  it('turns a timed event from the grid into an all-day one', () => {
    expect(resolveDrop(move(meeting, 'grid'), { kind: 'all-day', day: '2026-09-24' }, null)).toEqual({
      allDay: true, startDate: '2026-09-24', startTime: null, endDate: '2026-09-24', endTime: null,
    })
  })

  it('turns a timed event lasting several days, dropped on a band, into all-day over as many days', () => {
    // Grabbed by its first piece and dropped on the next day's band.
    const conference = base({ startDate: '2026-09-23', startTime: '09:00', endDate: '2026-09-25', endTime: '17:00' })
    expect(resolveDrop(move(conference, 'grid'), { kind: 'all-day', day: '2026-09-24' }, null)).toEqual({
      allDay: true, startDate: '2026-09-24', startTime: null, endDate: '2026-09-26', endTime: null,
    })
  })
})

describe('resolveDrop — tasks and meals', () => {
  it('moves a task to the target day whatever it was dropped on', () => {
    const task = base({ source: 'TASK', allDay: true, startTime: null, endTime: null })
    for (const target of [{ kind: 'day', day: '2026-09-27' }, { kind: 'all-day', day: '2026-09-27' },
      { kind: 'hours', day: '2026-09-27' }] as const) {
      expect(resolveDrop(move(task, 'grid'), target, 600)).toEqual({
        allDay: true, startDate: '2026-09-27', startTime: null, endDate: '2026-09-27', endTime: null,
      })
    }
  })
})

describe('resolveDrop — resizing', () => {
  it('moves the end, snapped, keeping the start', () => {
    expect(resolveDrop({ kind: 'resize-end', occurrence: meeting }, { kind: 'hours', day: '2026-09-23' }, 17 * 60))
      .toMatchObject({ startTime: '14:00', endTime: '17:00', endDate: '2026-09-23' })
  })

  it('moves the start, keeping the end', () => {
    expect(resolveDrop({ kind: 'resize-start', occurrence: meeting }, { kind: 'hours', day: '2026-09-23' }, 13 * 60))
      .toMatchObject({ startTime: '13:00', endTime: '15:30' })
  })

  it('never shrinks below 15 minutes nor lets one edge pass the other', () => {
    expect(resolveDrop({ kind: 'resize-end', occurrence: meeting }, { kind: 'hours', day: '2026-09-23' }, 10 * 60))
      .toMatchObject({ startTime: '14:00', endTime: '14:15' })
    expect(resolveDrop({ kind: 'resize-start', occurrence: meeting }, { kind: 'hours', day: '2026-09-23' }, 16 * 60))
      .toMatchObject({ startTime: '15:15', endTime: '15:30' })
  })

  it('moves the start into an earlier day when its handle is dropped on that day\'s column', () => {
    // An event lasting several days: its start handle can be pulled back a day.
    const conference = base({ startDate: '2026-09-23', startTime: '09:00', endDate: '2026-09-25', endTime: '17:00' })
    expect(resolveDrop({ kind: 'resize-start', occurrence: conference }, { kind: 'hours', day: '2026-09-22' }, 22 * 60))
      .toMatchObject({ startDate: '2026-09-22', startTime: '22:00', endDate: '2026-09-25', endTime: '17:00' })
    // …and pushed forward into a later day, still stopping 15 minutes before the end.
    expect(resolveDrop({ kind: 'resize-start', occurrence: conference }, { kind: 'hours', day: '2026-09-25' }, 17 * 60))
      .toMatchObject({ startDate: '2026-09-25', startTime: '16:45' })
  })

  it('writes an end stretched to midnight as the next day at 00:00', () => {
    expect(resolveDrop({ kind: 'resize-end', occurrence: meeting }, { kind: 'hours', day: '2026-09-23' }, 1440))
      .toMatchObject({ endDate: '2026-09-24', endTime: '00:00' })
  })

  it('resizes the last piece of an overnight event against its own day', () => {
    const party = base({ startDate: '2026-09-23', startTime: '22:00', endDate: '2026-09-24', endTime: '02:00' })
    expect(resolveDrop({ kind: 'resize-end', occurrence: party }, { kind: 'hours', day: '2026-09-24' }, 3 * 60))
      .toMatchObject({ endDate: '2026-09-24', endTime: '03:00', startTime: '22:00' })
  })
})

describe('resolveDrop — anchored on the piece that was grabbed', () => {
  const trip = base({ allDay: true, startTime: null, endTime: null, startDate: '2026-09-21', endDate: '2026-09-25' })
  const party = base({ startDate: '2026-09-23', startTime: '22:00', endDate: '2026-09-24', endTime: '02:00' })

  it('writes nothing when a trip is put back on the day it was grabbed by', () => {
    // A Mon–Fri trip grabbed by its Wednesday and put back used to move two days.
    for (const from of ['cell', 'band', 'row'] as const) {
      expect(resolveDrop(move(trip, from, '2026-09-23'), { kind: 'day', day: '2026-09-23' }, null)).toBeNull()
    }
    expect(resolveDrop(move(trip, 'band', '2026-09-23'), { kind: 'all-day', day: '2026-09-23' }, null)).toBeNull()
  })

  it('moves a trip by the days between the grabbed piece and the drop', () => {
    expect(resolveDrop(move(trip, 'cell', '2026-09-23'), { kind: 'day', day: '2026-09-24' }, null))
      .toMatchObject({ startDate: '2026-09-22', endDate: '2026-09-26' })
  })

  it('moves an overnight event by its second piece: an hour down is an hour later', () => {
    expect(resolveDrop(move(party, 'grid', '2026-09-24', 60), { kind: 'hours', day: '2026-09-24' }, 120)).toEqual({
      allDay: false, startDate: '2026-09-23', startTime: '23:00', endDate: '2026-09-24', endTime: '03:00',
    })
  })

  it('writes nothing when an overnight event is put back from its second piece', () => {
    expect(resolveDrop(move(party, 'grid', '2026-09-24', 60), { kind: 'hours', day: '2026-09-24' }, 60)).toBeNull()
    expect(resolveDrop(move(party, 'cell', '2026-09-24'), { kind: 'day', day: '2026-09-24' }, null)).toBeNull()
  })

  it('moves a block by how far the pointer travelled, keeping a start off the quarter hour', () => {
    const odd = base({ startTime: '14:10', endTime: '15:00' })
    // Grabbed at 14:15, dropped an hour lower on the next day.
    expect(resolveDrop(move(odd, 'grid', '2026-09-23', 855), { kind: 'hours', day: '2026-09-24' }, 915)).toEqual({
      allDay: false, startDate: '2026-09-24', startTime: '15:10', endDate: '2026-09-24', endTime: '16:00',
    })
    expect(resolveDrop(move(odd, 'grid', '2026-09-23', 855), { kind: 'hours', day: '2026-09-23' }, 855)).toBeNull()
  })

  it('carries a block up past midnight into the day before', () => {
    const early = base({ startDate: '2026-09-24', startTime: '00:00', endDate: '2026-09-24', endTime: '01:00' })
    // Grabbed at 00:30, dropped on the previous day's column at 23:30.
    expect(resolveDrop(move(early, 'grid', '2026-09-24', 30), { kind: 'hours', day: '2026-09-23' }, 1410)).toEqual({
      allDay: false, startDate: '2026-09-23', startTime: '23:00', endDate: '2026-09-24', endTime: '00:00',
    })
  })

  it('turns an evening ending at midnight into a one-day all-day event, not two', () => {
    const evening = base({ startTime: '22:00', endDate: '2026-09-24', endTime: '00:00' })
    expect(resolveDrop(move(evening, 'grid'), { kind: 'all-day', day: '2026-09-25' }, null)).toEqual({
      allDay: true, startDate: '2026-09-25', startTime: null, endDate: '2026-09-25', endTime: null,
    })
  })
})

describe('previewDrop — where the item is drawn while it is dragged', () => {
  it('gives the landing place even when it is where the item already is', () => {
    // The preview is drawn at home rather than vanishing; only the write is skipped.
    const home = { kind: 'day' as const, day: '2026-09-23' }
    expect(previewDrop(move(meeting, 'cell'), home, null)).toEqual({
      allDay: false, startDate: '2026-09-23', startTime: '14:00', endDate: '2026-09-23', endTime: '15:30',
    })
    expect(resolveDrop(move(meeting, 'cell'), home, null)).toBeNull()
  })

  it('gives nothing where a drop would mean nothing', () => {
    expect(previewDrop(move(meeting), { kind: 'hours', day: '2026-09-24' }, null)).toBeNull()
  })
})

describe('isUnchanged', () => {
  it('reads the API\'s seconds and the drag\'s minutes as the same time', () => {
    expect(isUnchanged(meeting, { allDay: false, startDate: '2026-09-23', startTime: '14:00', endDate: '2026-09-23', endTime: '15:30' })).toBe(true)
    expect(isUnchanged(meeting, { allDay: false, startDate: '2026-09-23', startTime: '14:15', endDate: '2026-09-23', endTime: '15:45' })).toBe(false)
  })
})

describe('sameSchedule', () => {
  it('tells when the landing slot has not changed, so the views are not redrawn for nothing', () => {
    const slot = { allDay: false, startDate: '2026-09-23', startTime: '14:00', endDate: '2026-09-23', endTime: '15:30' }
    expect(sameSchedule(slot, { ...slot })).toBe(true)
    expect(sameSchedule(null, null)).toBe(true)
    expect(sameSchedule(slot, null)).toBe(false)
    expect(sameSchedule(slot, { ...slot, startTime: '14:15' })).toBe(false)
  })
})
