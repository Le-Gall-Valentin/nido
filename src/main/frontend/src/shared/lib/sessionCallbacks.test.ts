import { describe, it, expect, vi, afterEach } from 'vitest'
import {
  setSessionExpiredCallback, triggerSessionExpired,
  setLoginSuccessCallback, notifyLoginSuccess,
  resetSessionCallbacks,
} from './sessionCallbacks'

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

  it('tells whoever registered that a login succeeded', () => {
    // The mirror of the above, and the reason this half exists: the auth store has to say "somebody
    // signed in" without knowing that an axios interceptor is what listens.
    const cb = vi.fn()
    setLoginSuccessCallback(cb)

    notifyLoginSuccess()

    expect(cb).toHaveBeenCalledTimes(1)
  })

  it('a login with nobody listening is not an error', () => {
    expect(() => notifyLoginSuccess()).not.toThrow()
  })

  it('resetSessionCallbacks clears the login side too', () => {
    const cb = vi.fn()
    setLoginSuccessCallback(cb)
    resetSessionCallbacks()

    notifyLoginSuccess()

    expect(cb).not.toHaveBeenCalled()
  })
})