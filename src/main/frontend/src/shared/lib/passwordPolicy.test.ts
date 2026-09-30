import { describe, expect, it } from 'vitest'
import { isValidPassword, passwordProblem } from './passwordPolicy'

describe('passwordProblem', () => {
  it('accepts 8 characters or more with an uppercase letter, a digit and a symbol', () => {
    expect(passwordProblem('LongEnough1!')).toBeNull()
    expect(isValidPassword('LongEnough1!')).toBe(true)
  })

  it('names what stops a password, the length first', () => {
    expect(passwordProblem('Ab1!')).toBe('too_short')
    expect(passwordProblem('longenough1!')).toBe('weak')
    expect(passwordProblem('LongEnough!!')).toBe('weak')
    expect(passwordProblem('LongEnough11')).toBe('weak')
    expect(passwordProblem('Aa1!' + 'x'.repeat(69))).toBe('too_long')
  })

  it('measures the upper bound in bytes, as bcrypt reads a password', () => {
    // 72 characters, 141 bytes: within a character count, past what the server can hash.
    expect(passwordProblem('Aé1!' + 'é'.repeat(68))).toBe('too_long')
    expect(isValidPassword('Aé1!' + 'é'.repeat(68))).toBe(false)
    expect(passwordProblem('Aé1!' + 'x'.repeat(67))).toBeNull()
    expect(passwordProblem('Aé1!' + 'x'.repeat(68))).toBe('too_long')
  })
})
