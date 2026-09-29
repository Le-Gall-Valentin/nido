export interface PasswordResetCapabilities {
  passwordReset: boolean
}

/** The server side of "forgot password". Every method answers the same way whoever is asked about. */
export interface IPasswordResetApi {
  capabilities(): Promise<PasswordResetCapabilities>
  requestReset(identifier: string): Promise<void>
  /** @throws InvalidResetLinkError when the link expired, was used, was replaced or never existed */
  checkToken(token: string): Promise<void>
  /** @throws InvalidResetLinkError, WeakPasswordError */
  confirmReset(token: string, newPassword: string): Promise<void>
}
