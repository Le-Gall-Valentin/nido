/**
 * Mirror of the backend password policy
 * (identity RegisterRequest / ChangePasswordRequest):
 * 8-72 chars, at least one uppercase, one digit, one special character.
 */
export const PASSWORD_MIN_LENGTH = 8
export const PASSWORD_MAX_LENGTH = 72
export const PASSWORD_REGEX = /^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).+$/

export function isValidPassword(password: string): boolean {
  return (
    password.length >= PASSWORD_MIN_LENGTH &&
    password.length <= PASSWORD_MAX_LENGTH &&
    PASSWORD_REGEX.test(password)
  )
}
