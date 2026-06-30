// @vitest-environment node
import { beforeEach, describe, expect, it, vi } from 'vitest'
import axios, { type AxiosError } from 'axios'
import type { LoginCredentials } from '../model/types'
import { authApi } from './authApi'
import { client } from '@/shared/api'
import { CredentialsError, NetworkError, RateLimitError, ServerError } from '../model/errors'

vi.mock('@/shared/api', () => ({
  client: {
    post: vi.fn(),
    get: vi.fn(),
  },
}))

const mockedClient = client as unknown as {
  post: ReturnType<typeof vi.fn>
  get: ReturnType<typeof vi.fn>
}

function makeAxiosError(status: number, message = 'error', headers: Record<string, string> = {}): AxiosError {
  return new axios.AxiosError(message, undefined, undefined, undefined, {
    status,
    data: {},
    headers,
    config: {} as never,
    statusText: String(status),
  })
}

describe('authApi', () => {
  beforeEach(() => {
    mockedClient.post.mockReset()
    mockedClient.get.mockReset()
  })

  it('login posts credentials and returns full user from response', async () => {
    const credentials: LoginCredentials = { username: 'user', password: 'secret' }
    const fullUser = { id: '1', username: 'user', role: 'USER', email: 'user@test.com', createdAt: '2024-01-01T00:00:00Z', totpEnabled: false }
    mockedClient.post.mockResolvedValue({ data: fullUser })

    await expect(authApi.login(credentials)).resolves.toEqual({ type: 'success', user: fullUser })
    expect(mockedClient.post).toHaveBeenCalledWith('/auth/login', credentials)
    expect(mockedClient.get).not.toHaveBeenCalled()
  })

  it('login returns { type: totp_required } when server responds with totpRequired:true', async () => {
    mockedClient.post.mockResolvedValue({ data: { totpRequired: true } })
    const result = await authApi.login({ username: 'u', password: 'p' })
    expect(result).toEqual({ type: 'totp_required' })
  })

  it('logout calls logout endpoint', async () => {
    mockedClient.post.mockResolvedValue({})

    await authApi.logout()

    expect(mockedClient.post).toHaveBeenCalledWith('/auth/logout')
    expect(mockedClient.post).toHaveBeenCalledTimes(1)
  })

  it('getMe reads current user endpoint', async () => {
    const user = { id: '1', username: 'user', role: 'ADMIN' }
    mockedClient.get.mockResolvedValue({ data: user })

    await expect(authApi.getMe()).resolves.toEqual(user)
    expect(mockedClient.get).toHaveBeenCalledWith('/users/me')
  })

  it('login throws CredentialsError on 401', async () => {
    mockedClient.post.mockRejectedValue(makeAxiosError(401))
    await expect(authApi.login({ username: 'u', password: 'p' })).rejects.toBeInstanceOf(CredentialsError)
  })

  it('login throws ServerError on 500', async () => {
    mockedClient.post.mockRejectedValue(makeAxiosError(500))
    await expect(authApi.login({ username: 'u', password: 'p' })).rejects.toBeInstanceOf(ServerError)
  })

  it('login throws RateLimitError on 429', async () => {
    mockedClient.post.mockRejectedValue(makeAxiosError(429))
    await expect(authApi.login({ username: 'u', password: 'p' })).rejects.toBeInstanceOf(RateLimitError)
  })

  it('login throws NetworkError when no response', async () => {
    mockedClient.post.mockRejectedValue(new Error('Network Error'))
    await expect(authApi.login({ username: 'u', password: 'p' })).rejects.toBeInstanceOf(NetworkError)
  })

  it('logout throws ServerError on 500', async () => {
    mockedClient.post.mockRejectedValue(makeAxiosError(500))
    await expect(authApi.logout()).rejects.toBeInstanceOf(ServerError)
  })

  it('logout succeeds silently on 4xx (token already gone)', async () => {
    mockedClient.post.mockRejectedValue(makeAxiosError(401))
    await expect(authApi.logout()).resolves.toBeUndefined()
  })

  it('logout throws NetworkError when no response', async () => {
    mockedClient.post.mockRejectedValue(new Error('Network Error'))
    await expect(authApi.logout()).rejects.toBeInstanceOf(NetworkError)
  })

  it('getMe throws CredentialsError on 401', async () => {
    mockedClient.get.mockRejectedValue(makeAxiosError(401))
    await expect(authApi.getMe()).rejects.toBeInstanceOf(CredentialsError)
  })

  it('getMe throws ServerError on 500', async () => {
    mockedClient.get.mockRejectedValue(makeAxiosError(500))
    await expect(authApi.getMe()).rejects.toBeInstanceOf(ServerError)
  })

  it('getMe throws NetworkError when no response', async () => {
    mockedClient.get.mockRejectedValue(new Error('Network Error'))
    await expect(authApi.getMe()).rejects.toBeInstanceOf(NetworkError)
  })

  it('login throws ServerError on 400 (validation error)', async () => {
    mockedClient.post.mockRejectedValue(makeAxiosError(400))
    await expect(authApi.login({ username: 'u', password: 'p' })).rejects.toBeInstanceOf(ServerError)
  })

  it('login throws ServerError on 403', async () => {
    mockedClient.post.mockRejectedValue(makeAxiosError(403))
    await expect(authApi.login({ username: 'u', password: 'p' })).rejects.toBeInstanceOf(ServerError)
  })

  it('login RateLimitError carries retryAfterSeconds from header', async () => {
    mockedClient.post.mockRejectedValue(makeAxiosError(429, 'Too Many Requests', { 'retry-after': '45' }))
    const caught = await authApi.login({ username: 'u', password: 'p' }).catch((e) => e)
    expect(caught).toBeInstanceOf(RateLimitError)
    expect((caught as RateLimitError).retryAfterSeconds).toBe(45)
  })

  it('login RateLimitError has null retryAfterSeconds when header absent', async () => {
    mockedClient.post.mockRejectedValue(makeAxiosError(429))
    const caught = await authApi.login({ username: 'u', password: 'p' }).catch((e) => e)
    expect(caught).toBeInstanceOf(RateLimitError)
    expect((caught as RateLimitError).retryAfterSeconds).toBeNull()
  })

  it('getMe throws ServerError on 403 (should not trigger logout)', async () => {
    mockedClient.get.mockRejectedValue(makeAxiosError(403))
    await expect(authApi.getMe()).rejects.toBeInstanceOf(ServerError)
  })
})
