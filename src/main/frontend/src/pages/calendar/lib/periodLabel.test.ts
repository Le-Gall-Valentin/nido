import { describe, it, expect } from 'vitest'
import { formatDay, formatPeriodLabel, formatTimeRange } from './periodLabel'

const FR = 'fr-FR'
const EN = 'en-GB'

describe('formatPeriodLabel — month', () => {
  it('names the month and its year', () => {
    expect(formatPeriodLabel('month', '2026-09-15', FR)).toBe('Septembre 2026')
    expect(formatPeriodLabel('month', '2026-09-15', EN)).toBe('September 2026')
  })

  it('names the month the anchor is in, whatever day of it the anchor is', () => {
    expect(formatPeriodLabel('month', '2026-09-01', FR)).toBe(formatPeriodLabel('month', '2026-09-30', FR))
  })

  it('crosses into the next year without confusion', () => {
    expect(formatPeriodLabel('month', '2027-01-04', FR)).toBe('Janvier 2027')
  })
})

describe('formatPeriodLabel — day', () => {
  it('gives the weekday, the day, the month and the year', () => {
    // 2026-09-15 is a Tuesday. English puts a comma after the weekday and French does not —
    // both come from the locale, and neither is ours to normalise.
    expect(formatPeriodLabel('day', '2026-09-15', FR)).toBe('Mardi 15 septembre 2026')
    expect(formatPeriodLabel('day', '2026-09-15', EN)).toBe('Tuesday, 15 September 2026')
  })
})

describe('formatDay', () => {
  it('writes a day in full as it reads inside a sentence, lower case kept', () => {
    // "Ouvrir le mardi 15 septembre 2026" — the heading's capital would read wrong mid-sentence.
    expect(formatDay('2026-09-15', FR)).toBe('mardi 15 septembre 2026')
    expect(formatDay('2026-09-15', EN)).toBe('Tuesday, 15 September 2026')
  })
})

describe('formatPeriodLabel — week', () => {
  it('writes the month once when the whole week shares it', () => {
    // Monday 2026-09-14 to Sunday 2026-09-20.
    expect(formatPeriodLabel('week', '2026-09-15', FR)).toBe('14 – 20 septembre 2026')
    expect(formatPeriodLabel('week', '2026-09-15', EN)).toBe('14 – 20 September 2026')
  })

  it('names both months when the week straddles them', () => {
    // Monday 2026-09-28 to Sunday 2026-10-04.
    expect(formatPeriodLabel('week', '2026-09-30', FR)).toBe('28 sept. – 4 oct. 2026')
  })

  it('names both years when the week straddles them', () => {
    // Monday 2026-12-28 to Sunday 2027-01-03.
    expect(formatPeriodLabel('week', '2026-12-30', FR)).toBe('28 déc. 2026 – 3 janv. 2027')
  })

  it('gives the same label for every day of the same week', () => {
    const monday = formatPeriodLabel('week', '2026-09-14', FR)
    const sunday = formatPeriodLabel('week', '2026-09-20', FR)
    expect(sunday).toBe(monday)
  })
})

describe('formatTimeRange', () => {
  const range = (startDate: string, startTime: string, endDate: string, endTime: string) =>
    ({ startDate, startTime, endDate, endTime })

  it('writes the two times of a single day', () => {
    expect(formatTimeRange(range('2026-10-19', '13:00:00', '2026-10-19', '14:30'), 'fr-FR')).toBe('13:00 – 14:30')
  })

  it('writes an end at midnight as 24:00, not as the next day', () => {
    expect(formatTimeRange(range('2026-10-20', '22:00', '2026-10-21', '00:00'), 'fr-FR')).toBe('22:00 – 24:00')
  })

  it('names the days when the range runs over several — "13:00 – 10:30" reads backwards', () => {
    // Non-breaking inside each end, so a narrow column wraps at the arrow, never inside "Mer. 21".
    expect(formatTimeRange(range('2026-10-19', '13:00', '2026-10-21', '10:30'), 'fr-FR'))
      .toBe('Lun.\u00a019\u00a013:00 → Mer.\u00a021\u00a010:30')
  })
})
