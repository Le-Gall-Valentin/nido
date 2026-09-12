import { describe, expect, it } from 'vitest'
import { spaceTimezoneOf } from './useSpaceTimezone'

describe('spaceTimezoneOf', () => {
  const spaces = [
    { id: 'home', timezone: 'Europe/Paris' },
    { id: 'perso', timezone: 'America/Toronto' },
  ]

  it('finds the calendar the named space keeps', () => {
    expect(spaceTimezoneOf(spaces, 'home')).toBe('Europe/Paris')
    expect(spaceTimezoneOf(spaces, 'perso')).toBe('America/Toronto')
  })

  it('falls back to the viewer own calendar while the spaces are still loading', () => {
    // Returning undefined rather than guessing a zone: todayIso() with no zone reads the viewer's
    // calendar, which is the best available answer for the fraction of a second before the list
    // arrives — and never a date from a zone nobody chose.
    expect(spaceTimezoneOf(undefined, 'home')).toBeUndefined()
    expect(spaceTimezoneOf(spaces, 'unknown-space')).toBeUndefined()
  })
})
