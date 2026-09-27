import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { ROUTES } from '@/shared/config'
import { Spinner } from '@/shared/ui'
import { useAuthGuard } from './useAuthGuard'

/**
 * Public pages (the login) for signed-out users only. A signed-in user — including one who has just
 * signed in, since the login page is still mounted when the session appears — goes home, where
 * DefaultRedirect opens their current space's dashboard: signing in and opening the app land on the
 * same page.
 */
export function PublicOnlyRoute({ children }: { children: ReactNode }) {
  const { isInitializing, isAuthenticated, t } = useAuthGuard()
  if (isInitializing) return <Spinner label={t('loading')} />
  return isAuthenticated ? <Navigate to={ROUTES.HOME} replace /> : <>{children}</>
}
