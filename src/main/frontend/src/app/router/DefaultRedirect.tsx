import { Navigate } from 'react-router-dom'
import { ROUTES } from '@/shared/config'
import { Spinner } from '@/shared/ui'
import { useAuthGuard } from './useAuthGuard'

export function DefaultRedirect() {
  const { isInitializing, isAuthenticated, t } = useAuthGuard()
  if (isInitializing) return <Spinner label={t('loading')} />
  return <Navigate to={isAuthenticated ? ROUTES.ACCOUNT : ROUTES.LOGIN} replace />
}