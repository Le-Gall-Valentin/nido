// @vitest-environment node
import { beforeEach, describe, expect, it, vi } from 'vitest'
import axios, { type AxiosError } from 'axios'
import { client } from '@/shared/api'
import { NetworkError, ServerError } from '@/shared/lib'
import { accountLanguageApi } from './accountLanguageApi'

vi.mock('@/shared/api', () => ({ client: { put: vi.fn() } }))

const mockedClient = client as unknown as { put: ReturnType<typeof vi.fn> }

function axiosError(status?: number): AxiosError {
  return new axios.AxiosError('error', undefined, undefined, undefined, status === undefined ? undefined : {
    status, data: {}, headers: {}, config: {} as never, statusText: String(status),
  })
}

describe('accountLanguageApi', () => {
  beforeEach(() => { mockedClient.put.mockReset() })

  it('records the language on the account', async () => {
    mockedClient.put.mockResolvedValue({ status: 204 })

    await accountLanguageApi.saveLanguage('en')

    expect(mockedClient.put).toHaveBeenCalledWith('/users/me/language', { language: 'en' })
  })

  it('reports an answer it did not expect as a server error, and no answer as a network error', async () => {
    mockedClient.put.mockRejectedValue(axiosError(500))
    await expect(accountLanguageApi.saveLanguage('fr')).rejects.toBeInstanceOf(ServerError)

    mockedClient.put.mockRejectedValue(axiosError())
    await expect(accountLanguageApi.saveLanguage('fr')).rejects.toBeInstanceOf(NetworkError)
  })
})
