import { AxiosError, AxiosHeaders } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { client } from '@/shared/api'
import { SettingsInvalidError } from '@/shared/lib'
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

  it('names the problems setting by setting', async () => {
    vi.mocked(client.put).mockRejectedValue(refused(400, { error_code: 'SETTINGS_INVALID', errors: { 'mail.port': 'out_of_range' } }))

    await expect(settingsApi.update('mail', {})).rejects.toBeInstanceOf(SettingsInvalidError)
  })
})
