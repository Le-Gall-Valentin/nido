import { describe, it, expect } from 'vitest'
import { formatRelativeTime } from './formatRelativeTime'

const NOW = new Date('2026-06-24T12:00:00Z')

describe('formatRelativeTime', () => {
  it('returns a days-ago string in French', () => {
    const out = formatRelativeTime('2026-06-21T12:00:00Z', 'fr', NOW)
    expect(out).toContain('3')
  })

  it('returns a string in English without throwing', () => {
    expect(typeof formatRelativeTime('2026-06-23T12:00:00Z', 'en', NOW)).toBe('string')
  })

  it('returns an em dash for an invalid date', () => {
    expect(formatRelativeTime('not-a-date', 'fr', NOW)).toBe('—')
  })
})