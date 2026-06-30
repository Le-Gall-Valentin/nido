import { describe, it, expect } from 'vitest'
import { formatUserDate } from './formatUserDate'

// Midday UTC keeps the calendar day stable across local timezones.
const DATE = '2024-02-15T12:00:00Z'

describe('formatUserDate', () => {
  it('formats day, localized short month and year for fr', () => {
    const formatted = formatUserDate(DATE, 'fr')
    expect(formatted).toContain('15')
    expect(formatted).toContain('2024')
    expect(formatted.toLowerCase()).toContain('fév') // "févr." in fr-FR
  })

  it('formats day, localized short month and year for en', () => {
    const formatted = formatUserDate(DATE, 'en')
    expect(formatted).toContain('15')
    expect(formatted).toContain('2024')
    expect(formatted).toContain('Feb') // en-GB short month
  })

  it('localizes the month differently for fr and en', () => {
    expect(formatUserDate(DATE, 'fr')).not.toBe(formatUserDate(DATE, 'en'))
  })

  it('returns a dash for an invalid date instead of "Invalid Date"', () => {
    expect(formatUserDate('', 'fr')).toBe('—')
    expect(formatUserDate('not-a-date', 'en')).toBe('—')
  })
})
