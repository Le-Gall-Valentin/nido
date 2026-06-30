/**
 * Lightweight email shape check, mirroring the backend `@Email` constraint
 * loosely on purpose: it only rejects values the server would also reject
 * (missing local part, missing/empty domain, whitespace), never narrower —
 * so the form can surface a hint before the round-trip without false rejects.
 */
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+$/

export function isValidEmail(email: string): boolean {
  return EMAIL_REGEX.test(email)
}
