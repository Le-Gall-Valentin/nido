import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { ROUTES } from '@/shared/config'
import { Alert, Spinner } from '@/shared/ui'
import { useMySpaces } from '@/features/space-switcher'
import { spacesPageApi } from '../api/spacesPageApi'
import type { ISpacesPageApi } from '../model/ISpacesPageApi'
import { SpacesPageApiProvider } from '../model/spacesPageApiContext'
import { useCreateSpace } from '../model/useSpaceMutations'
import { ReceivedInvitationsSection } from './ReceivedInvitationsSection'
import { SpaceListSection } from './SpaceListSection'
import { CreateSpaceModal } from './CreateSpaceModal'

interface SpacesPageProps {
  /** Composition seam: defaults to the real implementation; tests inject a fake. */
  api?: ISpacesPageApi
}

/**
 * Slice composition root: provisions the spaces page API at its own
 * boundary, so the app/router never has to know about this dependency.
 */
export function SpacesPage({ api = spacesPageApi }: SpacesPageProps = {}) {
  return (
    <SpacesPageApiProvider api={api}>
      <SpacesPageContent />
    </SpacesPageApiProvider>
  )
}

function SpacesPageContent() {
  const { t } = useTranslation('spaces')
  const navigate = useNavigate()
  const { data: spaces, isPending, isError } = useMySpaces()
  const createSpace = useCreateSpace()
  const [createOpen, setCreateOpen] = useState(false)

  return (
    <div className="mx-auto max-w-[820px] px-5 py-6 md:px-10 md:py-[34px]">
      <div className="mb-6">
        <p className="mb-1.5 text-[13px] font-semibold uppercase tracking-[0.05em] text-fg-3">{t('kicker')}</p>
        <h1 className="text-[32px] font-semibold tracking-tight text-fg-0">{t('title')}</h1>
        <p className="mt-1 text-[15px] text-fg-2">{t('subtitle')}</p>
      </div>

      <ReceivedInvitationsSection onAccepted={(spaceId) => navigate(ROUTES.spaceMembers(spaceId))} />

      {isPending && <Spinner label={t('loading')} fullscreen={false} />}
      {isError && <Alert variant="error">{t('list.load_error')}</Alert>}
      {!isPending && !isError && (
        <SpaceListSection
          spaces={spaces ?? []}
          onSelect={(spaceId) => navigate(ROUTES.spaceMembers(spaceId))}
          onCreateClick={() => setCreateOpen(true)}
        />
      )}

      {createOpen && (
        <CreateSpaceModal
          onClose={() => setCreateOpen(false)}
          onCreate={(input) => createSpace.mutateAsync(input)}
          onSuccess={(created) => { setCreateOpen(false); navigate(ROUTES.spaceMembers(created.id)) }}
        />
      )}
    </div>
  )
}
