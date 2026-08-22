import type { ReactNode } from 'react'
import { Navigate, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { isPersonal } from '@/entities/space'
import { useMySpaces } from '@/features/space-switcher'
import { Spinner } from '@/shared/ui'
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
  const { data: spaces, isLoading } = useMySpaces()
  const { t } = useTranslation('common')

  // Ce garde s'affiche dans le <main> du shell, deja contraint en hauteur :
  // la variante plein ecran du spinner y ferait deborder la zone scrollable.
  if (isLoading) return <Spinner label={t('loading')} fullscreen={false} />

  const exists = spaces?.some((space) => space.id === spaceId) ?? false
  if (exists) return <>{children}</>

  const personal = spaces?.find((space) => isPersonal(space))
  return <Navigate to={personal ? ROUTES.space(personal.id) : ROUTES.ACCOUNT} replace />
}
