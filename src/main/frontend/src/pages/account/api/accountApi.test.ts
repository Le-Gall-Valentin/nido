// @vitest-environment node
import { beforeEach, describe, expect, it, vi } from 'vitest'
import axios, { type AxiosError } from 'axios'
import { accountApi, ConflictError, InvalidCurrentPasswordError } from './accountApi'
import { client } from '@/shared/api'
import { NetworkError, RateLimitError, ServerError } from '@/shared/lib'

vi.mock('@/shared/api', () => ({
  client: {
    patch: vi.fn(),
  },
}))

const mockedClient = client as unknown as {
  patch: ReturnType<typeof vi.fn>
}

function makeAxiosError(status: number, headers: Record<string, string> = {}): AxiosError {
  return new axios.AxiosError('error', undefined, undefined, undefined, {
    status,
    data: {},
    headers,
    config: {} as never,
    statusText: String(status),
  })
}

describe('accountApi', () => {
  beforeEach(() => { mockedClient.patch.mockReset() })

  describe('updateProfile', () => {
    it('calls PATCH /users/me with username and email', async () => {
      mockedClient.patch.mockResolvedValue({ status: 204 })
      await accountApi.updateProfile('alice', 'alice@test.com')
      expect(mockedClient.patch).toHaveBeenCalledWith('/users/me', { username: 'alice', email: 'alice@test.com' })
    })

    it('resolves void on 204', async () => {
      mockedClient.patch.mockResolvedValue({ status: 204 })
      await expect(accountApi.updateProfile('a', 'a@test.com')).resolves.toBeUndefined()
    })

    it('throws ConflictError on 409', async () => {
      mockedClient.patch.mockRejectedValue(makeAxiosError(409))
      await expect(accountApi.updateProfile('a', 'a@test.com')).rejects.toBeInstanceOf(ConflictError)
    })

    it('throws ServerError on 500', async () => {
      mockedClient.patch.mockRejectedValue(makeAxiosError(500))
      await expect(accountApi.updateProfile('a', 'a@test.com')).rejects.toBeInstanceOf(ServerError)
    })

    it('throws RateLimitError on 429', async () => {
      mockedClient.patch.mockRejectedValue(makeAxiosError(429))
      await expect(accountApi.updateProfile('a', 'a@test.com')).rejects.toBeInstanceOf(RateLimitError)
    })

    it('throws NetworkError when no response', async () => {
      mockedClient.patch.mockRejectedValue(new Error('Network Error'))
      await expect(accountApi.updateProfile('a', 'a@test.com')).rejects.toBeInstanceOf(NetworkError)
    })
  })

  describe('changePassword', () => {
    it('calls PATCH /users/me/password with currentPassword and newPassword', async () => {
      mockedClient.patch.mockResolvedValue({ status: 204 })
      await accountApi.changePassword('old', 'New1!')
      expect(mockedClient.patch).toHaveBeenCalledWith('/users/me/password', { currentPassword: 'old', newPassword: 'New1!' })
    })

    it('resolves void on 204', async () => {
      mockedClient.patch.mockResolvedValue({ status: 204 })
      await expect(accountApi.changePassword('old', 'New1!')).resolves.toBeUndefined()
    })

    it('throws InvalidCurrentPasswordError on 422', async () => {
      mockedClient.patch.mockRejectedValue(makeAxiosError(422))
      await expect(accountApi.changePassword('wrong', 'New1!')).rejects.toBeInstanceOf(InvalidCurrentPasswordError)
    })

    it('throws ServerError on 500', async () => {
      mockedClient.patch.mockRejectedValue(makeAxiosError(500))
      await expect(accountApi.changePassword('old', 'New1!')).rejects.toBeInstanceOf(ServerError)
    })

    it('throws RateLimitError on 429', async () => {
      mockedClient.patch.mockRejectedValue(makeAxiosError(429))
      await expect(accountApi.changePassword('old', 'New1!')).rejects.toBeInstanceOf(RateLimitError)
    })

    it('throws NetworkError when no response', async () => {
      mockedClient.patch.mockRejectedValue(new Error('Network Error'))
      await expect(accountApi.changePassword('old', 'New1!')).rejects.toBeInstanceOf(NetworkError)
    })
  })
})