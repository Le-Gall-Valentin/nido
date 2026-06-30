type VoidCallback = () => void

let sessionExpiredCallback: VoidCallback | null = null

export function setSessionExpiredCallback(cb: VoidCallback | null): void {
  sessionExpiredCallback = cb
}

export function triggerSessionExpired(): void {
  sessionExpiredCallback?.()
}

export function resetSessionCallbacks(): void {
  sessionExpiredCallback = null
}