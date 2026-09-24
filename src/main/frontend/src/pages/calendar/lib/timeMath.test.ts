import { describe, it, expect } from 'vitest'
import { minutesAt, minutesToTime, snap, timeToMinutes } from './timeMath'

describe('timeMath', () => {
  it('reads a time with or without seconds', () => {
    expect(timeToMinutes('18:30')).toBe(1110)
    expect(timeToMinutes('18:30:00')).toBe(1110)
  })

  it('writes a time as HH:mm', () => {
    expect(minutesToTime(0)).toBe('00:00')
    expect(minutesToTime(1110)).toBe('18:30')
    expect(minutesToTime(1439)).toBe('23:59')
  })

  it('snaps to the nearest quarter hour', () => {
    expect(snap(7)).toBe(0)
    expect(snap(8)).toBe(15)
    expect(snap(1107)).toBe(1110)
  })

  it('turns a pointer position in a column into a snapped time', () => {
    // 60px per hour: 870px below the column's top is 14:30.
    expect(minutesAt(1000 + 870, 1000)).toBe(870)
    expect(minutesAt(1000 + 874, 1000)).toBe(870)
  })

  it('never reads a time outside the day, even with the pointer off the column', () => {
    expect(minutesAt(900, 1000)).toBe(0)
    expect(minutesAt(1000 + 2000, 1000)).toBe(1440)
  })
})
