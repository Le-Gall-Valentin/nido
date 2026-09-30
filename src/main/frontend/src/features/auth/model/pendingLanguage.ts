import type { Language } from '@/shared/lib'

const KEY_PREFIX = 'nido.pendingLanguage.'

/**
 * A language switch the server did not take, kept on this device for that account until it does —
 * otherwise the account's older language would win again at its next sign-in. One per account: a
 * switch made by one person never follows another onto the same device.
 */
export function readPendingLanguage(userId: string): Language | null {
  try {
    const stored = localStorage.getItem(KEY_PREFIX + userId)
    return stored === 'fr' || stored === 'en' ? stored : null
  } catch {
    return null
  }
}

export function keepPendingLanguage(userId: string, language: Language): void {
  try {
    localStorage.setItem(KEY_PREFIX + userId, language)
  } catch {
    // localStorage unavailable (private mode, quota exceeded): the switch lasts this session only
  }
}

export function forgetPendingLanguage(userId: string): void {
  try {
    localStorage.removeItem(KEY_PREFIX + userId)
  } catch {
    // localStorage unavailable (private mode, quota exceeded)
  }
}
