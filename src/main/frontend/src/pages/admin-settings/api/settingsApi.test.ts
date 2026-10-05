import { AxiosError, AxiosHeaders } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { client } from '@/shared/api'
import { MailTestFailedError, SettingLockedError, SettingsInvalidError } from '@/entities/instance-settings'
import { ServerError } from '@/shared/lib'
import { settingsApi } from './settingsApi'

vi.mock('@/shared/api', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/shared/api')>()
  return { ...actual, client: { get: vi.fn(), put: vi.fn(), delete: vi.fn(), post: vi.fn() } }
})

function refused(status: number, data: object = {}) {
  const config = { headers: new AxiosHeaders() }
  return new AxiosError('failed', 'ERR', config, null, { status, statusText: '', data, headers: {}, config })
}

describe('settingsApi', () => {
  beforeEach(() => vi.clearAllMocks())

  it('saves a block by its group and resets a setting by its key', async () => {
    vi.mocked(client.put).mockResolvedValue({ data: { groups: [] } })
    vi.mocked(client.delete).mockResolvedValue({ data: { groups: [] } })

    await settingsApi.update('mail', { 'mail.host': 'smtp.example.com' })
    await settingsApi.reset('mail', 'mail.password')

    expect(client.put).toHaveBeenCalledWith('/admin/settings/mail', { values: { 'mail.host': 'smtp.example.com' } })
    expect(client.delete).toHaveBeenCalledWith('/admin/settings/mail/mail.password')
  })

  it('reads the settings and sends the test with the values of the form', async () => {
    vi.mocked(client.get).mockResolvedValue({ data: { groups: [] } })
    vi.mocked(client.post).mockResolvedValue({ data: undefined })

    expect(await settingsApi.get()).toEqual({ groups: [] })
    await settingsApi.testMail({ 'mail.host': 'smtp.example.com' })

    expect(client.get).toHaveBeenCalledWith('/admin/settings')
    expect(client.post).toHaveBeenCalledWith('/admin/settings/mail/test', { values: { 'mail.host': 'smtp.example.com' } })
  })

  it('names the errors of the read, the reset and the test too', async () => {
    vi.mocked(client.get).mockRejectedValue(refused(500))
    vi.mocked(client.delete).mockRejectedValue(refused(409, { error_code: 'SETTING_LOCKED_BY_ENVIRONMENT', setting: 'api.swagger' }))
    vi.mocked(client.post).mockRejectedValue(refused(422, { error_code: 'MAIL_TEST_FAILED', reason: 'rejected', server_reply: '550 no' }))

    await expect(settingsApi.get()).rejects.toBeInstanceOf(ServerError)
    await expect(settingsApi.reset('api', 'api.swagger')).rejects.toBeInstanceOf(SettingLockedError)
    await expect(settingsApi.testMail({})).rejects.toBeInstanceOf(MailTestFailedError)
  })

  it('names the problems setting by setting', async () => {
    vi.mocked(client.put).mockRejectedValue(refused(400, { error_code: 'SETTINGS_INVALID', errors: { 'mail.port': 'out_of_range' } }))

    await expect(settingsApi.update('mail', {})).rejects.toBeInstanceOf(SettingsInvalidError)
  })
})
