import { create } from 'zustand'
import type { IAuthApi } from './IAuthApi'
import { clearSessionHint, hasSessionHint, setSessionHint } from '@/shared/lib'
import { notifyLoginSuccess } from '@/shared/api'
import type { User } from '@/entities/user'
import type { LoginCredentials, LoginOutcome } from './types'
import { CredentialsError } from './errors'

export interface AuthState {
  user: User | null
  isInitializing: boolean
}

export interface AuthActions {
  login: (credentials: LoginCredentials) => Promise<LoginOutcome>
  logout: () => Promise<void>
  initialize: (signal?: AbortSignal) => Promise<void>
  finalizeLogin: (user: User) => void
  patchUser: (partial: Partial<User>) => void
}

export function createAuthStore(api: IAuthApi) {
  // Aborted via signal on StrictMode remount — reset ensures the next mount can re-enter initialize().
  let initializationStarted = false

  return create<AuthState & AuthActions>((set) => ({
    user: null,
    isInitializing: true,

    async login(credentials: LoginCredentials): Promise<LoginOutcome> {
      const result = await api.login(credentials)
      if (result.type === 'totp_required') {
        return { kind: 'totp_required', username: credentials.username }
      }
      const { user } = result
      return { kind: 'enrollment_proposed', user }
    },

    finalizeLogin(user: User): void {
      notifyLoginSuccess()
      setSessionHint()
      set({ user })
    },

    patchUser(partial: Partial<User>): void {
      set(state => ({
        user: state.user ? { ...state.user, ...partial } : null,
      }))
    },

    async logout(): Promise<void> {
      try {
        await api.logout()
      } finally {
        clearSessionHint()
        set({ user: null })
      }
    },

    async initialize(signal?: AbortSignal): Promise<void> {
      if (initializationStarted) return
      initializationStarted = true

      // Reset synchronously on abort so a StrictMode remount can re-enter
      signal?.addEventListener('abort', () => { initializationStarted = false }, { once: true })

      if (!hasSessionHint()) {
        if (!signal?.aborted) set({ isInitializing: false })
        initializationStarted = false
        return
      }
      try {
        const user = await api.getMe()
        if (signal?.aborted) return
        set({ user, isInitializing: false })
      } catch (error) {
        if (signal?.aborted) return
        // Only invalidate the session on actual auth failure (401); leave hint intact on transient errors
        if (error instanceof CredentialsError) clearSessionHint()
        set({ user: null, isInitializing: false })
      } finally {
        // Allow re-entry after completion; abort handler covers the StrictMode case
        if (!signal?.aborted) initializationStarted = false
      }
    },
  }))
}