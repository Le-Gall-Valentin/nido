import type { CompleteSetupRequest, SetupKey, SetupMailTest, SetupStatus } from './types'

export interface ISetupApi {
  status(): Promise<SetupStatus>
  verifyCode(code: string): Promise<void>
  encryptionKey(code: string): Promise<SetupKey>
  testMail(request: SetupMailTest): Promise<void>
  complete(request: CompleteSetupRequest): Promise<void>
}
