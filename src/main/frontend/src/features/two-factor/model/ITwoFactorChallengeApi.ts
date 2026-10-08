import type { TwoFactorMethod, User } from '@/entities/user'
import type { ResendData } from './types'

/** The second step of a sign-in, under the challenge cookie the login set. */
export interface ITwoFactorChallengeApi {
  verify(method: TwoFactorMethod, code: string): Promise<User>
  /** Sends — or sends again — the sign-in code by mail. */
  sendMailCode(): Promise<ResendData>
}
