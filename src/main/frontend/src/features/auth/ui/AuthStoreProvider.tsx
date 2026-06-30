import { useRef, useEffect, type ReactNode } from 'react'
import { useStore } from 'zustand'
import { useTranslation } from 'react-i18next'
import { setSessionExpiredCallback } from '@/shared/lib'
import { Spinner } from '@/shared/ui'
import type { IAuthApi } from '../model/IAuthApi'
import { createAuthStore } from '../model/authStore'
import { AuthStoreContext, type AuthStoreApi } from '../model/authStoreContext'

interface Props {
  api: IAuthApi
  children: ReactNode
}

export function AuthStoreProvider({ api, children }: Props) {
  const storeRef = useRef<AuthStoreApi | null>(null)
  if (!storeRef.current) storeRef.current = createAuthStore(api)
  const store = storeRef.current

  const isInitializing = useStore(store, (s) => s.isInitializing)
  const { t } = useTranslation('common')

  useEffect(() => {
    let mounted = true
    const abortController = new AbortController()
    setSessionExpiredCallback(() => {
      const s = store.getState()
      if (mounted && !s.isInitializing) void s.logout()
    })
    void store.getState().initialize(abortController.signal)
    return () => {
      mounted = false
      abortController.abort()
      setSessionExpiredCallback(null)
    }
  }, [store])

  // This spinner absorbs the global init phase. ProtectedRoute and PublicOnlyRoute have
  // their own isInitializing branch only as a secondary safety net (normally unreachable).
  if (isInitializing) return <Spinner label={t('loading')} />

  return (
    <AuthStoreContext.Provider value={store}>
      {children}
    </AuthStoreContext.Provider>
  )
}