import { describe, it, expect, vi, afterEach } from 'vitest'
import { setSessionExpiredCallback, triggerSessionExpired, resetSessionCallbacks } from './sessionCallbacks'

afterEach(() => {
  resetSessionCallbacks()
})

describe('sessionCallbacks', () => {
  it('calls registered callback when session expires', () => {
    const cb = vi.fn()
    setSessionExpiredCallback(cb)
    triggerSessionExpired()
    expect(cb).toHaveBeenCalledTimes(1)
  })

  it('does nothing when no callback is registered', () => {
    expect(() => triggerSessionExpired()).not.toThrow()
  })

  it('resetSessionCallbacks prevents callback from being called', () => {
    const cb = vi.fn()
    setSessionExpiredCallback(cb)
    resetSessionCallbacks()
    triggerSessionExpired()
    expect(cb).not.toHaveBeenCalled()
  })
})