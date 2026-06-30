import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '@/features/auth'
import { isAdminRole } from '@/entities/user'
import { ROUTES } from '@/shared/config'

export function AdminRoute({ children }: { children: ReactNode }) {
  const user = useAuth((s) => s.user)
  return isAdminRole(user?.role) ? <>{children}</> : <Navigate to={ROUTES.ACCOUNT} replace />
}