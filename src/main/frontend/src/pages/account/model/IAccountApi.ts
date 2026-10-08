/**
 * What came of a profile change: saved — and, while mail was off, the code by mail removed since nothing could prove
 * the new address — or waiting for the code sent to the new address.
 */
export type ProfileUpdateResult =
  | { kind: 'saved'; mailMethodRemoved: boolean }
  | { kind: 'email_code_sent'; sentTo: string; resendAfterSeconds: number }

/**
 * Port for the current user's self-service account operations. The page
 * depends on this contract; the concrete axios-backed implementation is
 * injected (defaulting to accountApi), never imported as a hard dependency.
 */
export interface IAccountApi {
  /**
   * `currentPassword` is required by the server when the address changes (letter case aside). With the
   * code by mail on, the first call answers `email_code_sent` and saves nothing; the same call with
   * `emailCode` saves.
   */
  updateProfile(username: string, email: string, currentPassword?: string, emailCode?: string): Promise<ProfileUpdateResult>
  changePassword(currentPassword: string, newPassword: string): Promise<void>
}
