import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { ROUTES } from '@/shared/config'
import { Spinner } from '@/shared/ui'
import { useAuthGuard } from './useAuthGuard'

/**
 * The page ProtectedRoute remembered for after the login: a path of this app only — never another
 * site ("//host" is one), never the login page itself.
 */
function rememberedPage(state: unknown): string | null {
  const from = (state as { from?: { pathname?: unknown; search?: unknown; hash?: unknown } } | null)?.from
  const pathname = from?.pathname
  if (typeof pathname !== 'string' || !pathname.startsWith('/') || pathname.startsWith('//') || pathname === ROUTES.LOGIN) {
    return null
  }
  const search = typeof from?.search === 'string' ? from.search : ''
  const hash = typeof from?.hash === 'string' ? from.hash : ''
  return `${pathname}${search}${hash}`
}

/**
 * Public pages (the login) for signed-out users only. A signed-in user — including one who has just
 * signed in, since the login page is still mounted when the session appears — goes on to the page a
 * link was taking them to, or else home, where DefaultRedirect opens their current space's dashboard:
 * opening the app and signing in land on the same page.
 */
export function PublicOnlyRoute({ children }: { children: ReactNode }) {
  const { isInitializing, isAuthenticated, t } = useAuthGuard()
  const location = useLocation()
  if (isInitializing) return <Spinner label={t('loading')} />
  return isAuthenticated ? <Navigate to={rememberedPage(location.state) ?? ROUTES.HOME} replace /> : <>{children}</>
}
