import { describe, expect, it } from 'vitest'
import { formatTimezone } from './formatTimezone'

describe('formatTimezone', () => {
  it('shows the place, not the database path', () => {
    // "Europe/Paris" is what the API speaks; "Paris" is what somebody reads on a card.
    expect(formatTimezone('Europe/Paris')).toBe('Paris')
    expect(formatTimezone('America/Toronto')).toBe('Toronto')
  })

  it('reads underscores as the spaces they stand for', () => {
    expect(formatTimezone('America/Los_Angeles')).toBe('Los Angeles')
    expect(formatTimezone('America/New_York')).toBe('New York')
  })

  it('keeps the region when the place alone would be ambiguous', () => {
    // Several zones end in the same city name, and a household in one of them should not read the
    // other's. Where the identifier has three segments the middle one disambiguates.
    expect(formatTimezone('America/Argentina/Buenos_Aires')).toBe('Buenos Aires (Argentina)')
  })

  it('leaves a zone with no place to speak of alone', () => {
    expect(formatTimezone('UTC')).toBe('UTC')
  })
})
