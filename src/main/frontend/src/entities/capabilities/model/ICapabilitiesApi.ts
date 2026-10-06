/** What this installation can do, as the server says — mail can be switched on or off without a redeploy. */
export interface Capabilities {
  /** "Forgot password" is offered: mail can carry a reset link. */
  passwordReset: boolean
  /** A mail can leave this installation at all. */
  mail: boolean
}

export interface ICapabilitiesApi {
  capabilities(): Promise<Capabilities>
}
