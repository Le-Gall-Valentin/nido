import { beforeEach, describe, expect, it, vi } from 'vitest'
import { authApi } from '@/features/auth'
import { setSessionHint } from '@/shared/lib'
import { signInAfterSetup } from './signInAfterSetup'

vi.mock('@/features/auth', () => ({ authApi: { login: vi.fn() } }))
vi.mock('@/shared/lib', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/shared/lib')>()),
  setSessionHint: vi.fn(),
}))

describe('signInAfterSetup', () => {
  beforeEach(() => vi.clearAllMocks())

  it('signs the new administrator in and leaves the hint the app starts from', async () => {
    vi.mocked(authApi.login).mockResolvedValue({ type: 'success', user: {} as never })

    expect(await signInAfterSetup('jane', 'Str0ng!Password')).toBe(true)
    expect(authApi.login).toHaveBeenCalledWith({ identifier: 'jane', password: 'Str0ng!Password' })
    expect(setSessionHint).toHaveBeenCalled()
  })

  it('leaves it to the login page when the sign-in asks for more or fails', async () => {
    vi.mocked(authApi.login).mockResolvedValueOnce({ type: 'totp_required', username: 'jane' })
    expect(await signInAfterSetup('jane', 'p')).toBe(false)

    vi.mocked(authApi.login).mockRejectedValueOnce(new Error('offline'))
    expect(await signInAfterSetup('jane', 'p')).toBe(false)

    expect(setSessionHint).not.toHaveBeenCalled()
  })
})
