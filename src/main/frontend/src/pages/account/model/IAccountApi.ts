/**
 * Port for the current user's self-service account operations. The page
 * depends on this contract; the concrete axios-backed implementation is
 * injected (defaulting to accountApi), never imported as a hard dependency.
 */
export interface IAccountApi {
  /** `currentPassword` is required by the server when the address changes (letter case aside). */
  updateProfile(username: string, email: string, currentPassword?: string): Promise<void>
  changePassword(currentPassword: string, newPassword: string): Promise<void>
}
