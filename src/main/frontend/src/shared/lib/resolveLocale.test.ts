import { describe, expect, it } from 'vitest'
import { resolveLocale, toLanguage } from './resolveLocale'

describe('toLanguage', () => {
  it('reads a bare tag', () => {
    expect(toLanguage('fr')).toBe('fr')
    expect(toLanguage('en')).toBe('en')
  })

  it('reads a tag with a region, which is what the browser actually reports', () => {
    // The whole reason this exists. i18next-browser-languagedetector hands back what the browser
    // says — "fr-FR" on a French machine — and every formatter in the application used to compare
    // that to 'fr' with ===. The comparison failed, so French users read English months, English
    // relative times and dollars-style amounts.
    expect(toLanguage('fr-FR')).toBe('fr')
    expect(toLanguage('fr-CA')).toBe('fr')
    expect(toLanguage('en-GB')).toBe('en')
    expect(toLanguage('en-US')).toBe('en')
  })

  it('falls back to English for a language the application does not speak', () => {
    expect(toLanguage('de-DE')).toBe('en')
    expect(toLanguage('')).toBe('en')
    expect(toLanguage(undefined)).toBe('en')
  })
})

describe('resolveLocale', () => {
  it('gives Intl the full locale for the resolved language', () => {
    expect(resolveLocale('fr-FR')).toBe('fr-FR')
    expect(resolveLocale('fr')).toBe('fr-FR')
    expect(resolveLocale('en-US')).toBe('en-GB')
    expect(resolveLocale('de')).toBe('en-GB')
  })

  it('formats French the French way', () => {
    // The user-visible assertion, and the one that would have caught this: an amount and a date as
    // they appear on screen, not the language tag that produced them.
    const amount = new Intl.NumberFormat(resolveLocale('fr-FR'), { style: 'currency', currency: 'EUR' }).format(1234.5)
    expect(amount).toContain('1')
    expect(amount).toMatch(/€$/)
    expect(amount).toContain(',50')

    const date = new Date('2026-09-11T00:00:00Z')
      .toLocaleDateString(resolveLocale('fr-FR'), { day: '2-digit', month: 'long', year: 'numeric' })
    expect(date).toContain('septembre')
  })
})
