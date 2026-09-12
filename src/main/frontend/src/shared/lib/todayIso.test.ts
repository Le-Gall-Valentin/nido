import { describe, expect, it } from 'vitest'
import { todayIso } from './todayIso'

describe('todayIso', () => {
  it('reads the local calendar date, not the UTC one', () => {
    // 23:30 on the 9th, local time. toISOString() would report the 10th for any viewer
    // west of Greenwich — this is the bug the helper exists to avoid.
    const lateEvening = new Date(2026, 8, 9, 23, 30)

    expect(todayIso(lateEvening)).toBe('2026-09-09')
  })

  it('reads the local calendar date in the early morning too', () => {
    const earlyMorning = new Date(2026, 8, 9, 0, 30)

    expect(todayIso(earlyMorning)).toBe('2026-09-09')
  })

  it('pads months and days to two digits', () => {
    expect(todayIso(new Date(2026, 0, 5, 12, 0))).toBe('2026-01-05')
  })
})
