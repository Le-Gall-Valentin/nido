type VoidCallback = () => void

/**
 * The two things the session layer and the HTTP layer have to tell each other, without either
 * importing the other.
 *
 * <p>They are opposite directions of the same conversation. The interceptor discovers that a session
 * is over and the auth store has to hear it; the auth store knows a login succeeded and the
 * interceptor has to hear it, because it keeps a "already reported this expiry" flag that a fresh
 * session must clear. Routing both through here is what keeps a store — the model layer — from
 * importing an axios instance.
 */
let sessionExpiredCallback: VoidCallback | null = null
let loginSuccessCallback: VoidCallback | null = null

export function setSessionExpiredCallback(cb: VoidCallback | null): void {
  sessionExpiredCallback = cb
}

export function triggerSessionExpired(): void {
  sessionExpiredCallback?.()
}

export function setLoginSuccessCallback(cb: VoidCallback | null): void {
  loginSuccessCallback = cb
}

export function notifyLoginSuccess(): void {
  loginSuccessCallback?.()
}

export function resetSessionCallbacks(): void {
  sessionExpiredCallback = null
  loginSuccessCallback = null
}
