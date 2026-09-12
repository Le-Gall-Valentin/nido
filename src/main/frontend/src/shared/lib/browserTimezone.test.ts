import { describe, expect, it } from 'vitest'
import { browserTimezone } from './browserTimezone'

describe('browserTimezone', () => {
  it('reports an IANA identifier the API will accept', () => {
    // Intl reports the same vocabulary ZoneId parses, so nothing is translated between the two.
    // Asserted by shape rather than by value, because the suite runs wherever it runs.
    expect(browserTimezone()).toMatch(/^[A-Za-z]+(\/[A-Za-z0-9_+-]+)+$|^UTC$/)
  })

  it('falls back to a real zone when the browser cannot say', () => {
    // Old engines and locked-down environments return an empty string here. Sending that would be
    // refused by the API, and a space has to be creatable anyway.
    const blind = { resolvedOptions: () => ({ timeZone: '' }) } as unknown as Intl.DateTimeFormat

    expect(browserTimezone(() => blind)).toBe('Europe/Paris')
  })
})
