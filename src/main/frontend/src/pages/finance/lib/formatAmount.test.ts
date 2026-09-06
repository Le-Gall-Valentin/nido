import { describe, expect, it, vi } from 'vitest'
import i18next from 'i18next'
import { formatAmount } from './formatAmount'

vi.mock('i18next', () => ({ default: { language: 'fr' } }))

describe('formatAmount', () => {
  it('formats with French grouping/decimal style when the active language is fr', () => {
    i18next.language = 'fr'

    const result = formatAmount(1234.5)

    expect(result).toContain('€')
    expect(result).toMatch(/1\s234,50/)
  })

  it('formats with English/GB style for any other active language', () => {
    i18next.language = 'en'

    const result = formatAmount(1234.5)

    expect(result).toContain('€')
    expect(result).toMatch(/1,234\.50/)
  })

  it('keeps the currency as EUR regardless of the active language', () => {
    i18next.language = 'en'

    expect(formatAmount(1)).toContain('€')
  })
})
