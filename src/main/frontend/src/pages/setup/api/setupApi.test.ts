import { AxiosError, AxiosHeaders } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { client } from '@/shared/api'
import { MailTestFailedError, ServerError, SettingsInvalidError } from '@/shared/lib'
import { AdminRefusedError, KeyNotSavedError, SetupAlreadyDoneError, SetupCodeInvalidError } from '../model/errors'
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
  beforeEach(() => {
    vi.mocked(client.post).mockReset()
    vi.mocked(client.get).mockReset()
  })

  it('sends the code in the body, never in the address', async () => {
    vi.mocked(client.post).mockResolvedValue({ data: undefined })
    await setupApi.verifyCode('K7QM-3XRP-W9TD')
    expect(client.post).toHaveBeenCalledWith('/setup/verify-code', { code: 'K7QM-3XRP-W9TD' })
  })

  it('reads the status, the key, and sends the test and the end of the setup where the server expects them', async () => {
    vi.mocked(client.get).mockResolvedValue({ data: { required: true, lockedPublicUrl: null, mailLocked: false } })
    vi.mocked(client.post).mockResolvedValue({ data: { source: 'GENERATED', key: 'the-key' } })
    const test = { code: 'c', mail: { 'mail.host': 'smtp' }, publicUrl: 'https://nido.example.com', recipient: 'jane@example.fr', language: 'fr' as const }
    const complete = { code: 'c', admin: { username: 'jane', email: 'jane@example.fr', password: 'p', language: 'fr' as const },
      publicUrl: 'https://nido.example.com', mail: null, encryptionKeySaved: true }

    expect(await setupApi.status()).toEqual({ required: true, lockedPublicUrl: null, mailLocked: false })
    expect(await setupApi.encryptionKey('c')).toEqual({ source: 'GENERATED', key: 'the-key' })
    await setupApi.testMail(test)
    await setupApi.complete(complete)

    expect(client.get).toHaveBeenCalledWith('/setup/status')
    expect(client.post).toHaveBeenCalledWith('/setup/encryption-key', { code: 'c' })
    expect(client.post).toHaveBeenCalledWith('/setup/mail-test', test)
    expect(client.post).toHaveBeenCalledWith('/setup/complete', complete)
  })

  it('names the errors of every route, not only the code check', async () => {
    vi.mocked(client.get).mockRejectedValueOnce(refused(500))
    await expect(setupApi.status()).rejects.toBeInstanceOf(ServerError)
    vi.mocked(client.post).mockRejectedValueOnce(refused(404))
    await expect(setupApi.encryptionKey('x')).rejects.toBeInstanceOf(SetupAlreadyDoneError)
    vi.mocked(client.post).mockRejectedValueOnce(refused(422, { error_code: 'MAIL_TEST_FAILED', reason: 'timeout' }))
    await expect(setupApi.testMail({} as never)).rejects.toBeInstanceOf(MailTestFailedError)
  })

  it('names the setup errors', async () => {
    vi.mocked(client.post).mockRejectedValueOnce(refused(403, { error_code: 'SETUP_CODE_INVALID' }))
    await expect(setupApi.verifyCode('x')).rejects.toBeInstanceOf(SetupCodeInvalidError)
    vi.mocked(client.post).mockRejectedValueOnce(refused(404))
    await expect(setupApi.verifyCode('x')).rejects.toBeInstanceOf(SetupAlreadyDoneError)
    vi.mocked(client.post).mockRejectedValueOnce(refused(400, { error_code: 'ENCRYPTION_KEY_NOT_SAVED' }))
    await expect(setupApi.complete({} as never)).rejects.toBeInstanceOf(KeyNotSavedError)
    vi.mocked(client.post).mockRejectedValueOnce(refused(400, { error_code: 'INITIAL_ADMIN_REFUSED' }))
    await expect(setupApi.complete({} as never)).rejects.toBeInstanceOf(AdminRefusedError)
    vi.mocked(client.post).mockRejectedValueOnce(refused(400, { error_code: 'SETTINGS_INVALID', errors: { 'public-url': 'invalid_url' } }))
    await expect(setupApi.complete({} as never)).rejects.toBeInstanceOf(SettingsInvalidError)
  })
})
