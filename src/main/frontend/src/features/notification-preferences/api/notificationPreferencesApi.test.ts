// @vitest-environment node
import { beforeEach, describe, expect, it, vi } from 'vitest'
import axios, { type AxiosError } from 'axios'
import { client } from '@/shared/api'
import { NetworkError, RateLimitError, ServerError } from '@/shared/lib'
import { notificationPreferencesApi } from './notificationPreferencesApi'

vi.mock('@/shared/api', () => ({ client: { get: vi.fn(), put: vi.fn() } }))

const mocked = client as unknown as { get: ReturnType<typeof vi.fn>; put: ReturnType<typeof vi.fn> }

function axiosError(status?: number, headers: Record<string, string> = {}): AxiosError {
  return new axios.AxiosError('error', undefined, undefined, undefined, status === undefined ? undefined : {
    status, data: {}, headers, config: {} as never, statusText: String(status),
  })
}

describe('notificationPreferencesApi', () => {
  beforeEach(() => {
    mocked.get.mockReset()
    mocked.put.mockReset()
  })

  it('reads the account choices', async () => {
    const preferences = { channels: [{ channel: 'email', enabled: true }], types: [{ type: 'space.invitation', enabled: false }] }
    mocked.get.mockResolvedValue({ data: preferences })

    await expect(notificationPreferencesApi.get()).resolves.toEqual(preferences)
    expect(mocked.get).toHaveBeenCalledWith('/notifications/preferences')
  })

  it('switches a channel by its code', async () => {
    mocked.put.mockResolvedValue({ status: 204 })

    await notificationPreferencesApi.setChannel('email', false)

    expect(mocked.put).toHaveBeenCalledWith('/notifications/preferences/channels/email', { enabled: false })
  })

  it('switches a kind by its whole dotted code', async () => {
    mocked.put.mockResolvedValue({ status: 204 })

    await notificationPreferencesApi.setType('space.invitation', true)

    expect(mocked.put).toHaveBeenCalledWith('/notifications/preferences/types/space.invitation', { enabled: true })
  })

  it('reads 429 as too many changes, with the delay', async () => {
    mocked.put.mockRejectedValue(axiosError(429, { 'retry-after': '30' }))

    const error = await notificationPreferencesApi.setType('space.invitation', true).catch((e: unknown) => e)

    expect(error).toBeInstanceOf(RateLimitError)
    expect((error as RateLimitError).retryAfterSeconds).toBe(30)
  })

  it('reads any other answer as a server error, and no answer as the network', async () => {
    mocked.get.mockRejectedValueOnce(axiosError(500))
    mocked.get.mockRejectedValueOnce(axiosError())

    await expect(notificationPreferencesApi.get()).rejects.toBeInstanceOf(ServerError)
    await expect(notificationPreferencesApi.get()).rejects.toBeInstanceOf(NetworkError)
  })
})
