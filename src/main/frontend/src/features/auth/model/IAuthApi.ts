import type { User } from '@/entities/user'
import type { LoginApiResult, LoginCredentials } from './types'

export interface IAuthApi {
  login(credentials: LoginCredentials): Promise<LoginApiResult>
  logout(): Promise<void>
  getMe(): Promise<User>
}