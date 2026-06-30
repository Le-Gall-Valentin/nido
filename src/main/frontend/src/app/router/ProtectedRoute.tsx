import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { ROUTES } from '@/shared/config'
import { Spinner } from '@/shared/ui'
import { useAuthGuard } from './useAuthGuard'

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { isInitializing, isAuthenticated, t } = useAuthGuard()
  if (isInitializing) return <Spinner label={t('loading')} />
  return isAuthenticated ? <>{children}</> : <Navigate to={ROUTES.LOGIN} replace />
}