import { client, toInstanceError } from '@/shared/api'
import type { ISetupApi } from '../model/ISetupApi'
import type { SetupKey, SetupStatus } from '../model/types'
import { KeyNotSavedError, SetupAlreadyDoneError, SetupCodeInvalidError } from '../model/errors'

function setupError(error: unknown): Error {
  return toInstanceError(error, (status, code) => {
    if (code === 'SETUP_CODE_INVALID') return new SetupCodeInvalidError()
    if (code === 'ENCRYPTION_KEY_NOT_SAVED') return new KeyNotSavedError()
    if (status === 404) return new SetupAlreadyDoneError()
    return null
  })
}

export const setupApi: ISetupApi = {
  async status() {
    try {
      const { data } = await client.get<SetupStatus>('/setup/status')
      return data
    } catch (error) {
      throw setupError(error)
    }
  },
  async verifyCode(code) {
    try {
      await client.post('/setup/verify-code', { code })
    } catch (error) {
      throw setupError(error)
    }
  },
  async encryptionKey(code) {
    try {
      const { data } = await client.post<SetupKey>('/setup/encryption-key', { code })
      return data
    } catch (error) {
      throw setupError(error)
    }
  },
  async testMail(request) {
    try {
      await client.post('/setup/mail-test', request)
    } catch (error) {
      throw setupError(error)
    }
  },
  async complete(request) {
    try {
      await client.post('/setup/complete', request)
    } catch (error) {
      throw setupError(error)
    }
  },
}
