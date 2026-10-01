/**
 * Mirror of the backend username rule (identity domain, Username): 3 to 50 characters once the
 * surrounding spaces are dropped, none of them '@' — sign-in reads a value holding one as an address.
 */
export const USERNAME_MIN_LENGTH = 3
export const USERNAME_MAX_LENGTH = 50

export type UsernameProblem = 'too_short' | 'too_long' | 'has_at'

/** What stops a username from being accepted, the length first — or null when nothing does. */
export function usernameProblem(username: string): UsernameProblem | null {
  const name = username.trim()
  if (name.length < USERNAME_MIN_LENGTH) return 'too_short'
  if (name.length > USERNAME_MAX_LENGTH) return 'too_long'
  if (name.includes('@')) return 'has_at'
  return null
}

export function isValidUsername(username: string): boolean {
  return usernameProblem(username) === null
}
