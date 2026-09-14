import { describe, expect, it } from 'vitest'
import { monthIso, todayIso } from './todayIso'

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

  it('reads the date in the space timezone, not the viewer own', () => {
    // The case the household cares about: a member in Toronto looking at a space that keeps Paris
    // time. At 20:30 in Toronto it is already half past two the next morning in Paris, and the
    // household's day has turned over — the tasks due "today" are the new day's.
    const torontoEvening = new Date('2026-09-12T00:30:00Z')

    expect(todayIso(torontoEvening, 'Europe/Paris')).toBe('2026-09-12')
    expect(todayIso(torontoEvening, 'America/Toronto')).toBe('2026-09-11')
  })

  it('falls back to the viewer calendar when no zone is given', () => {
    // Every existing caller passes no zone, and must keep behaving as it did until it is migrated.
    const localNoon = new Date(2026, 8, 9, 12, 0)

    expect(todayIso(localNoon)).toBe('2026-09-09')
  })

  it('formats a month in the space timezone too', () => {
    // FinancePage opens on a month. On the 31st at 20:30 in Toronto, Paris is already the 1st of
    // the next month — the page must open on the household's month.
    const lastDayOfMonth = new Date('2026-10-01T00:30:00Z')

    expect(monthIso(lastDayOfMonth, 'Europe/Paris')).toBe('2026-10')
    expect(monthIso(lastDayOfMonth, 'America/Toronto')).toBe('2026-09')
  })

  it('pads months and days to two digits', () => {
    expect(todayIso(new Date(2026, 0, 5, 12, 0))).toBe('2026-01-05')
  })
})
