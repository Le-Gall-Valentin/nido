// @vitest-environment node
import { beforeEach, describe, expect, it, vi } from 'vitest'
import axios, { type AxiosError } from 'axios'
import { accountApi, ConflictError, EmailCodeInvalidError, InvalidCurrentPasswordError } from './accountApi'
import { ResendTooSoonError, SendLimitError } from '@/features/two-factor'
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

function makeAxiosError(status: number, headers: Record<string, string> = {}, data: Record<string, unknown> = {}): AxiosError {
  return new axios.AxiosError('error', undefined, undefined, undefined, {
    status,
    data,
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

    it('sends the current password when there is one', async () => {
      mockedClient.patch.mockResolvedValue({ status: 204 })
      await accountApi.updateProfile('alice', 'new@test.com', 'secret')
      expect(mockedClient.patch).toHaveBeenCalledWith('/users/me', { username: 'alice', email: 'new@test.com', currentPassword: 'secret' })
    })

    it('throws InvalidCurrentPasswordError on 422', async () => {
      mockedClient.patch.mockRejectedValue(makeAxiosError(422))
      await expect(accountApi.updateProfile('a', 'new@test.com', 'wrong')).rejects.toBeInstanceOf(InvalidCurrentPasswordError)
    })

    it('resolves saved on 204', async () => {
      mockedClient.patch.mockResolvedValue({ status: 204 })
      await expect(accountApi.updateProfile('a', 'a@test.com')).resolves.toEqual({ kind: 'saved' })
    })

    it('a 202 says a code went to the new address', async () => {
      mockedClient.patch.mockResolvedValue({ status: 202, data: { emailCodeRequired: true, sentTo: 'new@test.com', resendAfterSeconds: 60 } })

      await expect(accountApi.updateProfile('alice', 'new@test.com', 'secret'))
        .resolves.toEqual({ kind: 'email_code_sent', sentTo: 'new@test.com', resendAfterSeconds: 60 })
    })

    it('sends the code with the same change', async () => {
      mockedClient.patch.mockResolvedValue({ status: 204 })

      await accountApi.updateProfile('alice', 'new@test.com', 'secret', '004213')

      expect(mockedClient.patch).toHaveBeenCalledWith('/users/me',
        { username: 'alice', email: 'new@test.com', currentPassword: 'secret', emailCode: '004213' })
    })

    it('a wrong code is its own error, not a password or session one', async () => {
      mockedClient.patch.mockRejectedValue(makeAxiosError(400, {}, { error_code: 'email_code_invalid' }))

      await expect(accountApi.updateProfile('a', 'new@test.com', 'secret', '000000')).rejects.toBeInstanceOf(EmailCodeInvalidError)
    })

    it('a code asked again too soon or too often says when', async () => {
      mockedClient.patch.mockRejectedValueOnce(makeAxiosError(429, { 'retry-after': '30' }, { error_code: 'resend_too_soon', retryAfterSeconds: 30 }))
      await expect(accountApi.updateProfile('a', 'new@test.com', 'secret')).rejects.toEqual(new ResendTooSoonError(30))

      mockedClient.patch.mockRejectedValueOnce(makeAxiosError(429, { 'retry-after': '600' }, { error_code: 'send_limit_reached', retryAfterSeconds: 600 }))
      await expect(accountApi.updateProfile('a', 'new@test.com', 'secret')).rejects.toEqual(new SendLimitError(600))
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