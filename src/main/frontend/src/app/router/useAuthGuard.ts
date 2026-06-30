import { useShallow } from 'zustand/react/shallow'
import { useTranslation } from 'react-i18next'
import { useAuth } from '@/features/auth'

interface AuthGuardResult {
  isInitializing: boolean
  isAuthenticated: boolean
  t: (key: string) => string
}

export function useAuthGuard(): AuthGuardResult {
  const { isInitializing, isAuthenticated } = useAuth(
    useShallow((s) => ({ isInitializing: s.isInitializing, isAuthenticated: s.user !== null }))
  )
  const { t } = useTranslation('common')
  return { isInitializing, isAuthenticated, t }
}