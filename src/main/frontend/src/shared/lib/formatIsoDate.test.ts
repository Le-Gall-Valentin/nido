import { describe, expect, it, vi } from 'vitest'
import i18next from 'i18next'
import { formatIsoDate } from './formatIsoDate'

vi.mock('i18next', () => ({ default: { language: 'fr' } }))

describe('formatIsoDate', () => {
  it('writes a calendar day in full, in the language of the interface', () => {
    i18next.language = 'fr'
    expect(formatIsoDate('2026-09-30')).toBe('30 septembre 2026')

    i18next.language = 'en'
    expect(formatIsoDate('2026-09-30')).toBe('30 September 2026')
  })

  it('writes the day it is given, whatever the time zone of the browser', () => {
    // A calendar day has no time: read as midnight in the browser's zone, it would be the 29th west of Greenwich.
    i18next.language = 'fr'
    expect(formatIsoDate('2026-01-01')).toBe('1 janvier 2026')
  })
})
