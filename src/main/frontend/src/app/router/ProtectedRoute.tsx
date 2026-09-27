import { useMemo, type ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { ROUTES } from '@/shared/config'
import { Spinner } from '@/shared/ui'
import { useAuthGuard } from './useAuthGuard'

/**
 * Signed-in users only. A signed-out visitor goes to the login page with the page they were going to
 * in the router state — never in the URL — so that signing in takes them there (PublicOnlyRoute).
 * Not after the user signed themselves out: whoever signs in next starts from the dashboard, not from
 * the page the last session left.
 */
export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { isInitializing, isAuthenticated, signedOut, t } = useAuthGuard()
  const { pathname, search, hash } = useLocation()
  // Stable from one render to the next: <Navigate> navigates again whenever its state changes, and a
  // guard still mounted on the login page would then never stop.
  const state = useMemo(
    () => (signedOut || pathname === ROUTES.LOGIN ? undefined : { from: { pathname, search, hash } }),
    [signedOut, pathname, search, hash])
  if (isInitializing) return <Spinner label={t('loading')} />
  if (isAuthenticated) return <>{children}</>
  return <Navigate to={ROUTES.LOGIN} replace state={state} />
}
