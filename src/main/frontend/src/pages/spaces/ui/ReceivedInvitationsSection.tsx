import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Check } from 'lucide-react'
import { SpaceAvatar, SpaceRolePill } from '@/entities/space'
import { Alert, Button } from '@/shared/ui'
import { formatRelativeTime } from '@/shared/lib'
import { useReceivedInvitations } from '../model/useReceivedInvitations'
import { useAcceptInvitation } from '../model/useSpaceMutations'
import { mapSpaceErrorToKey } from '../lib/mapSpaceErrorToKey'

interface ReceivedInvitationsSectionProps {
  /** Navigation belongs to the caller: the joined context is not known until acceptance succeeds. */
  onAccepted: (spaceId: string) => void
}

/** Only renders when there is at least one pending invitation to act on. */
export function ReceivedInvitationsSection({ onAccepted }: ReceivedInvitationsSectionProps) {
  const { t, i18n } = useTranslation('spaces')
  const { data: invitations } = useReceivedInvitations()
  const acceptInvitation = useAcceptInvitation()
  const [errorKey, setErrorKey] = useState<string[] | null>(null)

  if (!invitations || invitations.length === 0) return null

  // Navigation is awaited on the mutation itself — including the
  // invalidations useAcceptInvitation's onSuccess returns — rather than
  // fired from a callback race that could run before the refetch settles.
  async function handleAccept(invitationId: string) {
    setErrorKey(null)
    try {
      const result = await acceptInvitation.mutateAsync(invitationId)
      onAccepted(result.spaceId)
    } catch (error) {
      setErrorKey(mapSpaceErrorToKey(error, 'accept'))
    }
  }

  return (
    <section className="mb-8">
      <h2 className="mb-3 text-[13px] font-semibold uppercase tracking-[0.05em] text-fg-3">
        {t('received.title')}
      </h2>

      {errorKey && <Alert variant="error" className="mb-3">{t(errorKey)}</Alert>}

      <ul className="flex flex-col gap-2.5">
        {invitations.map((invitation) => {
          const accepting = acceptInvitation.isPending && acceptInvitation.variables === invitation.invitationId
          return (
            <li
              key={invitation.invitationId}
              className="flex flex-wrap items-center gap-3.5 rounded-2xl border border-border bg-bg-1 px-[18px] py-3.5"
            >
              <SpaceAvatar space={{ accent: invitation.spaceAccent, glyph: invitation.spaceGlyph }} size="md" />
              <div className="min-w-0 flex-1">
                <div className="text-[14.5px] font-semibold text-fg-0 truncate">{invitation.spaceName}</div>
                <div className="text-[12.5px] text-fg-3 truncate">
                  {t('received.expires', { time: formatRelativeTime(invitation.expiresAt, i18n.language) })}
                </div>
              </div>
              <SpaceRolePill role={invitation.role} label={t(`space:role.${invitation.role}`)} />
              <Button
                onClick={() => handleAccept(invitation.invitationId)}
                isLoading={accepting}
                className="border-transparent font-semibold text-bg-0"
                style={{ background: 'var(--color-status-green)' }}
              >
                <Check className="size-4" />
                {t('received.action_accept')}
              </Button>
            </li>
          )
        })}
      </ul>
    </section>
  )
}
