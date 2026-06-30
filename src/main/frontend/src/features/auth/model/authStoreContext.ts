import { createContext, useContext } from 'react'
import { useStore } from 'zustand'
import type { StoreApi } from 'zustand'
import type { AuthState, AuthActions } from './authStore'

export type AuthStoreApi = StoreApi<AuthState & AuthActions>

export const AuthStoreContext = createContext<AuthStoreApi | null>(null)

export function useAuth<T>(selector: (state: AuthState & AuthActions) => T): T {
  const store = useContext(AuthStoreContext)
  if (!store) throw new Error('useAuth must be used within AuthStoreProvider')
  return useStore(store, selector)
}