import { Navigate } from 'react-router-dom'
import { useCurrentSpaceId } from '@/features/space-switcher'
import { ROUTES } from '@/shared/config'
import { Spinner } from '@/shared/ui'
import { useAuthGuard } from './useAuthGuard'

/**
 * The unscoped entry point: lands on the same space useCurrentSpaceId
 * resolves for the persistent sidebar (remembered context, falling back to
 * the personal space), so the nav and this redirect never disagree about
 * "your current space." The list is only fetched once authenticated.
 *
 * Unlike SpaceRoute it does decide on a cached list that is still refetching,
 * and it falls back to the account page when the list cannot be loaded at
 * all. Both are deliberate: the user asked for no context in particular, so
 * landing somewhere live beats holding an empty route, and SpaceRoute guards
 * the destination anyway if the remembered context turns out to be gone.
 */
export function DefaultRedirect() {
  const { isInitializing, isAuthenticated, t } = useAuthGuard()
  const { spaceId, isLoading } = useCurrentSpaceId({ enabled: isAuthenticated })

  if (isInitializing) return <Spinner label={t('loading')} />
  if (!isAuthenticated) return <Navigate to={ROUTES.LOGIN} replace />
  if (isLoading) return <Spinner label={t('loading')} />

  return <Navigate to={spaceId ? ROUTES.space(spaceId) : ROUTES.ACCOUNT} replace />
}
