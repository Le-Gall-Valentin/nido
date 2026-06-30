import { describe, it, expect } from 'vitest'
import { parseRetryAfter } from './parseRetryAfter'

describe('parseRetryAfter', () => {
  it('parses a numeric retry-after header', () => {
    expect(parseRetryAfter({ 'retry-after': '30' })).toBe(30)
  })

  it('returns null when retry-after header is absent', () => {
    expect(parseRetryAfter({})).toBeNull()
  })

  it('returns null when headers are undefined', () => {
    expect(parseRetryAfter(undefined)).toBeNull()
  })

  it('returns null when retry-after is not a number', () => {
    expect(parseRetryAfter({ 'retry-after': 'abc' })).toBeNull()
  })

  it('returns null when retry-after is empty string', () => {
    expect(parseRetryAfter({ 'retry-after': '' })).toBeNull()
  })
})