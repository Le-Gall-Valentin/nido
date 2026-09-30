/**
 * Mirror of the backend password policy (shared/validation/StrongPassword): 8 characters or more,
 * 72 bytes at most, with at least one uppercase letter, one digit and one special character.
 *
 * The upper bound is in bytes because bcrypt reads 72 bytes of a password and the server refuses to
 * hash more: an accented letter takes two, an emoji four, so 72 accented letters are 141 bytes.
 */
export const PASSWORD_MIN_LENGTH = 8
export const PASSWORD_MAX_BYTES = 72
export const PASSWORD_REGEX = /^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).+$/

export type PasswordProblem = 'too_short' | 'too_long' | 'weak'

const encoder = new TextEncoder()

/** What stops a password from being accepted, the length first — or null when nothing does. */
export function passwordProblem(password: string): PasswordProblem | null {
  if (password.length < PASSWORD_MIN_LENGTH) return 'too_short'
  if (encoder.encode(password).length > PASSWORD_MAX_BYTES) return 'too_long'
  if (!PASSWORD_REGEX.test(password)) return 'weak'
  return null
}

export function isValidPassword(password: string): boolean {
  return passwordProblem(password) === null
}
