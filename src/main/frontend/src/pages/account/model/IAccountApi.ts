/**
 * Port for the current user's self-service account operations. The page
 * depends on this contract; the concrete axios-backed implementation is
 * injected (defaulting to accountApi), never imported as a hard dependency.
 */
export interface IAccountApi {
  updateProfile(username: string, email: string): Promise<void>
  changePassword(currentPassword: string, newPassword: string): Promise<void>
}
