import { useShallow } from 'zustand/react/shallow'
import { useTranslation } from 'react-i18next'
import { useAuth } from '@/features/auth'

interface AuthGuardResult {
  isInitializing: boolean
  isAuthenticated: boolean
  /** The user signed themselves out (see the auth store's `signedOut`). */
  signedOut: boolean
  t: (key: string) => string
}

export function useAuthGuard(): AuthGuardResult {
  const { isInitializing, isAuthenticated, signedOut } = useAuth(
    useShallow((s) => ({ isInitializing: s.isInitializing, isAuthenticated: s.user !== null, signedOut: s.signedOut }))
  )
  const { t } = useTranslation('common')
  return { isInitializing, isAuthenticated, signedOut, t }
}