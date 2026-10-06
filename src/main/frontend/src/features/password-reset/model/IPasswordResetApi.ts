/** The server side of "forgot password". Every method answers the same way whoever is asked about. */
export interface IPasswordResetApi {
  requestReset(identifier: string): Promise<void>
  /** @throws InvalidLinkError when the link expired, was used, was replaced or never existed */
  checkToken(token: string): Promise<void>
  /** @throws InvalidLinkError, WeakPasswordError */
  confirmReset(token: string, newPassword: string): Promise<void>
}
