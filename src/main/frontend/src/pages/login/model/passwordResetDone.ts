/** Router state the reset page leaves on the login page's history entry after a successful reset. */
export const PASSWORD_RESET_DONE_STATE = { passwordReset: 'done' } as const

export function isPasswordResetDone(state: unknown): boolean {
  return (state as { passwordReset?: unknown } | null)?.passwordReset === PASSWORD_RESET_DONE_STATE.passwordReset
}
