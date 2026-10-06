/** The server side of an invitation link: whose it is, and choosing the first password with it. */
export interface IAccountInvitationApi {
  /**
   * @returns the username of the invited account
   * @throws InvalidLinkError when the link expired, was used, was replaced, never existed, or its account is off
   */
  checkInvitation(token: string): Promise<string>
  /** @throws InvalidLinkError, WeakPasswordError */
  acceptInvitation(token: string, password: string): Promise<void>
}
