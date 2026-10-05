import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '@/features/auth'
import { ROUTES } from '@/shared/config'

/** SUPER_ADMIN only: the instance settings concern the whole installation. Rendered under ProtectedRoute, like AdminRoute. */
export function SuperAdminRoute({ children }: { children: ReactNode }) {
  const user = useAuth((s) => s.user)
  return user?.role === 'SUPER_ADMIN' ? <>{children}</> : <Navigate to={ROUTES.ACCOUNT} replace />
}
