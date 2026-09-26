import { describe, it, expect } from 'vitest'
import {
  addDaysIso, formatDayMonth, formatLongDay, formatMonthName, formatMonthYear, formatShortDay, formatWeekday, shortTime,
} from './dates'

describe('dashboard dates', () => {
  it('titles the page with the long day, capitalised', () => {
    expect(formatLongDay('2026-09-26', 'fr-FR')).toBe('Samedi 26 septembre')
  })

  it('formats short and medium dates on the calendar date, whatever the machine zone', () => {
    expect(formatShortDay('2026-09-28', 'fr-FR')).toBe('lun. 28')
    expect(formatDayMonth('2026-09-22', 'fr-FR')).toBe('22 sept.')
    expect(formatWeekday('2026-10-01', 'fr-FR')).toBe('jeudi')
    expect(formatMonthYear('2027-06-30', 'fr-FR')).toBe('juin 2027')
    expect(formatMonthName('2026-09', 'fr-FR')).toBe('Septembre')
  })

  it('adds days across a month end', () => {
    expect(addDaysIso('2026-09-28', 6)).toBe('2026-10-04')
    expect(addDaysIso('2026-12-31', 1)).toBe('2027-01-01')
  })

  it('shows a time as HH:mm whether or not the seconds were sent', () => {
    expect(shortTime('19:30:00')).toBe('19:30')
    expect(shortTime('07:05')).toBe('07:05')
  })
})
