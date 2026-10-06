import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { capabilitiesApi, usePasswordResetAvailability, type ICapabilitiesApi } from '@/entities/capabilities'
import { ROUTES } from '@/shared/config'

interface Props {
  children: ReactNode
  api?: ICapabilitiesApi
}

/**
 * The forgot and reset pages exist only where mail does: on an installation without SMTP (or when
 * the server cannot be asked), a bookmarked or typed URL lands on the login page instead.
 */
export function RequirePasswordReset({ children, api = capabilitiesApi }: Props) {
  const availability = usePasswordResetAvailability(api)
  if (availability === 'loading') return null
  if (availability === 'unavailable') return <Navigate to={ROUTES.LOGIN} replace />
  return <>{children}</>
}
