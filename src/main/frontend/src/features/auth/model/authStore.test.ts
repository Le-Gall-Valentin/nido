import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createAuthStore } from './authStore'
import type { IAuthApi } from './IAuthApi'
import { clearSessionHint, hasSessionHint, setSessionHint } from '@/shared/lib'
import { notifyLoginSuccess } from '@/shared/api'
import { CredentialsError, NetworkError, ServerError } from './errors'

vi.mock('@/shared/lib', async (importActual) => {
  const actual = await importActual<typeof import('@/shared/lib')>()
  return {
    ...actual,
    setSessionHint: vi.fn(),
    clearSessionHint: vi.fn(),
    hasSessionHint: vi.fn(),
  }
})

vi.mock('@/shared/api', () => ({
  notifyLoginSuccess: vi.fn(),
  client: { post: vi.fn(), get: vi.fn() },
}))

const mockedSetSessionHint = vi.mocked(setSessionHint)
const mockedClearSessionHint = vi.mocked(clearSessionHint)
const mockedHasSessionHint = vi.mocked(hasSessionHint)
const mockedNotifyLoginSuccess = vi.mocked(notifyLoginSuccess)

function createApiMock(): IAuthApi {
  return {
    login: vi.fn(),
    logout: vi.fn(),
    getMe: vi.fn(),
  }
}

describe('authStore', () => {
  beforeEach(() => {
    mockedSetSessionHint.mockReset()
    mockedClearSessionHint.mockReset()
    mockedHasSessionHint.mockReset()
    mockedNotifyLoginSuccess.mockReset()
  })

  it('login returns enrollment_proposed without calling notifyLoginSuccess', async () => {
    const api = createApiMock()
    const user = { id: '1', username: 'user', role: 'USER' as const, email: 'u@test.com', createdAt: '2024-01-01T00:00:00Z', totpEnabled: false }
    vi.mocked(api.login).mockResolvedValue({ type: 'success', user })
    const store = createAuthStore(api)

    const outcome = await store.getState().login({ username: 'user', password: 'secret' })

    expect(api.login).toHaveBeenCalledWith({ username: 'user', password: 'secret' })
    expect(mockedNotifyLoginSuccess).not.toHaveBeenCalled()
    expect(mockedSetSessionHint).not.toHaveBeenCalled()
    expect(store.getState().user).toBeNull()
    expect(outcome).toEqual({ kind: 'enrollment_proposed', user })
  })

  it('login returns totp_required with username and does not set user', async () => {
    const api = createApiMock()
    vi.mocked(api.login).mockResolvedValue({ type: 'totp_required' })
    const store = createAuthStore(api)

    const outcome = await store.getState().login({ username: 'alice', password: 'secret' })

    expect(outcome).toEqual({ kind: 'totp_required', username: 'alice' })
    expect(mockedSetSessionHint).not.toHaveBeenCalled()
    expect(mockedNotifyLoginSuccess).not.toHaveBeenCalled()
    expect(store.getState().user).toBeNull()
  })


  it('finalizeLogin sets user and session hint', () => {
    const api = createApiMock()
    const store = createAuthStore(api)
    const user = { id: '1', username: 'user', role: 'USER' as const, email: 'u@test.com', createdAt: '2024-01-01T00:00:00Z', totpEnabled: false }

    store.getState().finalizeLogin(user)

    expect(mockedSetSessionHint).toHaveBeenCalledTimes(1)
    expect(store.getState().user).toEqual(user)
    expect(mockedNotifyLoginSuccess).toHaveBeenCalledTimes(1)
  })

  it('logout clears auth state even when API fails', async () => {
    const api = createApiMock()
    vi.mocked(api.logout).mockRejectedValue(new Error('network'))
    const store = createAuthStore(api)

    await expect(store.getState().logout()).rejects.toThrow('network')

    expect(mockedClearSessionHint).toHaveBeenCalledTimes(1)
    expect(store.getState().user).toBeNull()
  })

  describe('initialize', () => {
    it('skips getMe and sets isInitializing=false when no session hint', async () => {
      const api = createApiMock()
      mockedHasSessionHint.mockReturnValue(false)
      const store = createAuthStore(api)

      await store.getState().initialize()

      expect(api.getMe).not.toHaveBeenCalled()
      expect(store.getState().isInitializing).toBe(false)
      expect(store.getState().user).toBeNull()
    })

    it('calls getMe and hydrates user when session hint is set', async () => {
      const api = createApiMock()
      mockedHasSessionHint.mockReturnValue(true)
      vi.mocked(api.getMe).mockResolvedValue({ id: '1', username: 'admin', role: 'ADMIN', email: 'admin@test.com', createdAt: '2024-01-01T00:00:00Z', totpEnabled: false })
      const store = createAuthStore(api)

      await store.getState().initialize()

      expect(api.getMe).toHaveBeenCalledTimes(1)
      expect(store.getState().isInitializing).toBe(false)
      expect(store.getState().user?.role).toBe('ADMIN')
      expect(mockedSetSessionHint).toHaveBeenCalledTimes(0)
    })

    it('clears state and hint when getMe returns 401 (session expired)', async () => {
      const api = createApiMock()
      mockedHasSessionHint.mockReturnValue(true)
      vi.mocked(api.getMe).mockRejectedValue(new CredentialsError())
      const store = createAuthStore(api)

      await store.getState().initialize()

      expect(mockedClearSessionHint).toHaveBeenCalledTimes(1)
      expect(store.getState().isInitializing).toBe(false)
      expect(store.getState().user).toBeNull()
    })

    it('keeps session hint when getMe fails with transient error (5xx or network)', async () => {
      for (const error of [new ServerError(), new NetworkError()]) {
        const api = createApiMock()
        mockedHasSessionHint.mockReturnValue(true)
        mockedClearSessionHint.mockClear()
        vi.mocked(api.getMe).mockRejectedValue(error)
        const store = createAuthStore(api)

        await store.getState().initialize()

        expect(mockedClearSessionHint).not.toHaveBeenCalled()
        expect(store.getState().isInitializing).toBe(false)
        expect(store.getState().user).toBeNull()
      }
    })

    it('re-initializes after first call is aborted (StrictMode remount)', async () => {
      const api = createApiMock()
      const controller1 = new AbortController()
      mockedHasSessionHint.mockReturnValue(true)

      let resolveGetMe!: (value: { id: string; username: string; role: 'USER'; email: string; createdAt: string; totpEnabled: boolean }) => void
      vi.mocked(api.getMe)
        .mockImplementationOnce(
          () => new Promise((resolve) => { resolveGetMe = resolve as typeof resolveGetMe })
        )
        .mockResolvedValueOnce({ id: '2', username: 'alice', role: 'USER', email: 'alice@test.com', createdAt: '2024-01-01T00:00:00Z', totpEnabled: false })

      const store = createAuthStore(api)

      // First call — will be aborted before getMe resolves
      const initPromise1 = store.getState().initialize(controller1.signal)
      controller1.abort()
      resolveGetMe({ id: '1', username: 'alice', role: 'USER', email: 'alice@test.com', createdAt: '2024-01-01T00:00:00Z', totpEnabled: false })
      await initPromise1 // wait for the aborted call to fully settle

      // Second call — must proceed and complete normally
      await store.getState().initialize(new AbortController().signal)

      expect(store.getState().isInitializing).toBe(false)
      expect(store.getState().user?.username).toBe('alice')
    })

    it('does not update store when signal is aborted before getMe resolves', async () => {
      const api = createApiMock()
      const abortController = new AbortController()

      let resolveGetMe!: (value: { id: string; username: string; role: 'USER'; email: string; createdAt: string; totpEnabled: boolean }) => void
      vi.mocked(api.getMe).mockImplementation(
        () => new Promise((resolve) => { resolveGetMe = resolve as typeof resolveGetMe })
      )
      mockedHasSessionHint.mockReturnValue(true)

      const store = createAuthStore(api)
      const initPromise = store.getState().initialize(abortController.signal)

      // Abort before getMe resolves
      abortController.abort()
      resolveGetMe({ id: '1', username: 'alice', role: 'USER', email: 'alice@test.com', createdAt: '2024-01-01T00:00:00Z', totpEnabled: false })
      await initPromise

      // isInitializing stays true because we never set it to false (aborted)
      expect(store.getState().isInitializing).toBe(true)
      expect(store.getState().user).toBeNull()
    })
  })

  it('patchUser merges partial into the current user', () => {
    const api = createApiMock()
    const store = createAuthStore(api)
    const user = {
      id: '1', username: 'user', role: 'USER' as const,
      email: 'u@test.com', createdAt: '2024-01-01T00:00:00Z', totpEnabled: false,
    }
    store.setState({ user })

    store.getState().patchUser({ username: 'updated', totpEnabled: true })

    expect(store.getState().user?.username).toBe('updated')
    expect(store.getState().user?.totpEnabled).toBe(true)
    expect(store.getState().user?.email).toBe('u@test.com')
  })

  it('patchUser is a no-op when user is null', () => {
    const api = createApiMock()
    const store = createAuthStore(api)

    store.getState().patchUser({ username: 'ghost' })

    expect(store.getState().user).toBeNull()
  })
})