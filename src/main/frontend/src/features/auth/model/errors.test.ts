import { describe, it, expect } from 'vitest'
import { CredentialsError, NetworkError, ServerError, RateLimitError } from './errors'

describe('error classes', () => {
  it('CredentialsError has correct name', () => {
    expect(new CredentialsError().name).toBe('CredentialsError')
  })

  it('NetworkError has correct name', () => {
    expect(new NetworkError().name).toBe('NetworkError')
  })

  it('ServerError has correct name', () => {
    expect(new ServerError().name).toBe('ServerError')
  })

  it('RateLimitError has correct name', () => {
    expect(new RateLimitError().name).toBe('RateLimitError')
  })
})