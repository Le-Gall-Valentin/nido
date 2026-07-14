const SESSION_HINT_KEY = 'nido.hasSession'

export function setSessionHint(): void {
  try {
    localStorage.setItem(SESSION_HINT_KEY, '1')
  } catch {
    // localStorage unavailable (private mode, quota exceeded)
  }
}

export function clearSessionHint(): void {
  try {
    localStorage.removeItem(SESSION_HINT_KEY)
  } catch {
    // localStorage unavailable (private mode, quota exceeded)
  }
}

export function hasSessionHint(): boolean {
  try {
    return localStorage.getItem(SESSION_HINT_KEY) === '1'
  } catch {
    return false
  }
}
