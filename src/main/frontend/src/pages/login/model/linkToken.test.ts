import { describe, expect, it } from 'vitest'
import { tokenInHash } from './linkToken'

describe('tokenInHash', () => {
  it('reads the token after the hash', () => {
    expect(tokenInHash('#token=abc')).toBe('abc')
  })

  it('ignores what a messaging app adds after it', () => {
    expect(tokenInHash('#token=abc&utm_source=chat')).toBe('abc')
  })

  it('has no token without one, or with a blank one', () => {
    expect(tokenInHash('')).toBeNull()
    expect(tokenInHash('#token=')).toBeNull()
    expect(tokenInHash('#token=%20')).toBeNull()
  })
})
