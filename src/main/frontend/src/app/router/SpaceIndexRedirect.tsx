import { Navigate, useParams } from 'react-router-dom'
import { ROUTES } from '@/shared/config'

/**
 * `/s/:spaceId` has no page of its own: it opens the space's dashboard. Opening the app
 * (DefaultRedirect), the personal-space fallback of SpaceRoute and every link to a space all go
 * through here, so they all land on the page that says what matters in that space.
 */
export function SpaceIndexRedirect() {
  const { spaceId = '' } = useParams<{ spaceId: string }>()
  return <Navigate to={ROUTES.spaceDashboard(spaceId)} replace />
}
