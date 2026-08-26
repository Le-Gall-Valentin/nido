import type { ReactNode } from 'react'
import { Navigate, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { isPersonal } from '@/entities/space'
import { useMySpaces } from '@/features/space-switcher'
import { Alert, Spinner } from '@/shared/ui'
import { ROUTES } from '@/shared/config'

/**
 * Guards a /s/:spaceId subtree: waits for the caller's space list before
 * deciding anything, so a slow network never ejects a legitimate user mid-
 * refresh. Once loaded, a context missing from the list — gone, or never
 * accessible to this account — sends the user back to their personal space
 * rather than surfacing a permission message: the backend already collapses
 * "not a member" and "does not exist" into the same 404 on purpose.
 */
export function SpaceRoute({ children }: { children: ReactNode }) {
  const { spaceId } = useParams<{ spaceId: string }>()
  const { data: spaces, isLoading, isFetching, isError } = useMySpaces()
  const { t } = useTranslation('common')

  // This guard renders inside the shell's <main>, already height-constrained:
  // the fullscreen spinner variant would overflow the scrollable area there.
  if (isLoading) return <Spinner label={t('loading')} fullscreen={false} />

  const exists = spaces?.some((space) => space.id === spaceId) ?? false
  if (exists) return <>{children}</>

  // The list failed to load: this says nothing about whether the context
  // itself exists, so it must never be read as "gone" and eject the user.
  if (isError) return <Alert variant="error">{t('error.spaces_unavailable')}</Alert>

  // Not found in the cached list, but a refetch (e.g. the invalidation that
  // follows accepting an invitation) is in flight and may still reveal it —
  // only the case that would otherwise redirect waits on it, not every
  // background refresh.
  if (isFetching) return <Spinner label={t('loading')} fullscreen={false} />

  const personal = spaces?.find((space) => isPersonal(space))
  return <Navigate to={personal ? ROUTES.space(personal.id) : ROUTES.ACCOUNT} replace />
}
