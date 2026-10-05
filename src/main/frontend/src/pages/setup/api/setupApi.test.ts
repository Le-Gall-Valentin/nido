import { AxiosError, AxiosHeaders } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { client } from '@/shared/api'
import { SettingsInvalidError } from '@/shared/lib'
import { KeyNotSavedError, SetupAlreadyDoneError, SetupCodeInvalidError } from '../model/errors'
import { setupApi } from './setupApi'

vi.mock('@/shared/api', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/shared/api')>()
  return { ...actual, client: { get: vi.fn(), post: vi.fn() } }
})

function refused(status: number, data: object = {}) {
  const config = { headers: new AxiosHeaders() }
  return new AxiosError('failed', 'ERR', config, null, { status, statusText: '', data, headers: {}, config })
}

describe('setupApi', () => {
  beforeEach(() => vi.mocked(client.post).mockReset())

  it('sends the code in the body, never in the address', async () => {
    vi.mocked(client.post).mockResolvedValue({ data: undefined })
    await setupApi.verifyCode('K7QM-3XRP-W9TD')
    expect(client.post).toHaveBeenCalledWith('/setup/verify-code', { code: 'K7QM-3XRP-W9TD' })
  })

  it('names the setup errors', async () => {
    vi.mocked(client.post).mockRejectedValueOnce(refused(403, { error_code: 'SETUP_CODE_INVALID' }))
    await expect(setupApi.verifyCode('x')).rejects.toBeInstanceOf(SetupCodeInvalidError)
    vi.mocked(client.post).mockRejectedValueOnce(refused(404))
    await expect(setupApi.verifyCode('x')).rejects.toBeInstanceOf(SetupAlreadyDoneError)
    vi.mocked(client.post).mockRejectedValueOnce(refused(400, { error_code: 'ENCRYPTION_KEY_NOT_SAVED' }))
    await expect(setupApi.complete({} as never)).rejects.toBeInstanceOf(KeyNotSavedError)
    vi.mocked(client.post).mockRejectedValueOnce(refused(400, { error_code: 'SETTINGS_INVALID', errors: { 'public-url': 'invalid_url' } }))
    await expect(setupApi.complete({} as never)).rejects.toBeInstanceOf(SettingsInvalidError)
  })
})
