import { useEffect } from 'react'
import { Navigate } from 'react-router-dom'
import { isPersonal } from '@/entities/space'
import { activeSpaceStore, useMySpaces } from '@/features/space-switcher'
import { ROUTES } from '@/shared/config'
import { Spinner } from '@/shared/ui'
import { useAuthGuard } from './useAuthGuard'

/**
 * The unscoped entry point: restores the last context the user was in
 * (validated against their current list, since it may have disappeared —
 * left, deleted, revoked), falling back to their personal space. The list
 * is only fetched once authenticated, and never decided on before it settles
 * — same rule as SpaceRoute, so a slow network never bounces the user to the
 * wrong place.
 */
export function DefaultRedirect() {
  const { isInitializing, isAuthenticated, t } = useAuthGuard()
  const { data: spaces, isLoading } = useMySpaces({ enabled: isAuthenticated })
  const lastSpaceId = activeSpaceStore((s) => s.lastSpaceId)

  const remembered = lastSpaceId ? spaces?.find((space) => space.id === lastSpaceId) : undefined
  const stale = !!lastSpaceId && !!spaces && !remembered

  useEffect(() => {
    if (stale) activeSpaceStore.getState().forget()
  }, [stale])

  if (isInitializing) return <Spinner label={t('loading')} />
  if (!isAuthenticated) return <Navigate to={ROUTES.LOGIN} replace />
  if (isLoading) return <Spinner label={t('loading')} />

  if (remembered) return <Navigate to={ROUTES.space(remembered.id)} replace />

  const personal = spaces?.find((space) => isPersonal(space))
  return <Navigate to={personal ? ROUTES.space(personal.id) : ROUTES.ACCOUNT} replace />
}
