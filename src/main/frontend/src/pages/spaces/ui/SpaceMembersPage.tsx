import { useNavigate } from 'react-router-dom'
import { ROUTES } from '@/shared/config'
import { SpaceMembersApiProvider , spaceApi , SpaceApiProvider } from '@/entities/space'
import { useActiveSpace } from '@/features/space-switcher'
import type { ISpaceApi } from '@/entities/space'
import { SpaceDetailSection } from './SpaceDetailSection'

interface SpaceMembersPageProps {
  /** Composition seam: defaults to the real implementation; tests inject a fake. */
  api?: ISpaceApi
}

/**
 * Mounted under the scoped /s/:spaceId subtree (see SpaceRoute/SpaceLayout).
 * The context comes from the URL through useActiveSpace(), never from a
 * local selection state — that is what makes a link to a group shareable.
 */
export function SpaceMembersPage({ api = spaceApi }: SpaceMembersPageProps = {}) {
  return (
    <SpaceApiProvider api={api}>
      <SpaceMembersApiProvider api={api}>
        <SpaceMembersPageContent />
      </SpaceMembersApiProvider>
    </SpaceApiProvider>
  )
}

function SpaceMembersPageContent() {
  const { spaceId } = useActiveSpace()
  const navigate = useNavigate()

  // SpaceRoute already guarantees spaceId resolves to an accessible context
  // before this renders; this is only a defensive fallback.
  if (!spaceId) return null

  return (
    <div className="mx-auto max-w-[900px] px-5 py-6 md:px-10 md:py-[34px]">
      <SpaceDetailSection
        spaceId={spaceId}
        onLeft={() => navigate(ROUTES.SPACES)}
        onDeleted={() => navigate(ROUTES.SPACES)}
      />
    </div>
  )
}
