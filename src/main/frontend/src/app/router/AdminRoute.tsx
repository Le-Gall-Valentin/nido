import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '@/features/auth'
import { isAdminRole } from '@/entities/user'
import { ROUTES } from '@/shared/config'

/**
 * Admin-only subtree.
 *
 * <p>Unlike {@link ProtectedRoute} and {@link PublicOnlyRoute} this one does not branch on
 * {@code isInitializing}, and deliberately so: it renders inside ProtectedRoute, which does, under
 * AuthStoreProvider, which holds the whole tree behind a spinner until initialisation ends. Two
 * ancestors already guarantee a settled session by the time this runs, so a third check would be a
 * branch no path can reach. Raised once as a finding — recorded here so it is not raised again.
 */
export function AdminRoute({ children }: { children: ReactNode }) {
  const user = useAuth((s) => s.user)
  return isAdminRole(user?.role) ? <>{children}</> : <Navigate to={ROUTES.ACCOUNT} replace />
}