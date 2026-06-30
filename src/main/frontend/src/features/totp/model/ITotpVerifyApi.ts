import type { User } from '@/entities/user'
export interface ITotpVerifyApi {
  verify(code: string): Promise<User>
}