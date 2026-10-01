import { describe, it, expect } from 'vitest'
import { isValidUsername, usernameProblem } from './usernamePolicy'

describe('usernamePolicy — mirror of the backend Username', () => {
  it('accepts 3 to 50 characters without @', () => {
    expect(usernameProblem('jane.doe')).toBeNull()
    expect(usernameProblem('a'.repeat(50))).toBeNull()
  })

  it('measures the name without its surrounding spaces, as the server does', () => {
    expect(usernameProblem('  ab  ')).toBe('too_short')
    expect(usernameProblem(' abc ')).toBeNull()
  })

  it('names the length first, then the @', () => {
    expect(usernameProblem('')).toBe('too_short')
    expect(usernameProblem('ab')).toBe('too_short')
    expect(usernameProblem('a'.repeat(51))).toBe('too_long')
    expect(usernameProblem('a@')).toBe('too_short')
    expect(usernameProblem('jane@home')).toBe('has_at')
  })

  it('isValidUsername is the absence of a problem', () => {
    expect(isValidUsername('jane')).toBe(true)
    expect(isValidUsername('jane@home')).toBe(false)
  })
})
