import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Copy, Check, X } from 'lucide-react'
import { SpaceRolePill, type SpaceInvitation } from '@/entities/space'
import { formatRelativeTime } from '@/shared/lib'

const STATUS_CLASS: Record<SpaceInvitation['status'], string> = {
  PENDING: 'bg-status-orange-dim text-status-orange',
  ACCEPTED: 'bg-status-green-dim text-status-green',
  REVOKED: 'bg-bg-3 text-fg-3',
}

interface InvitationListProps {
  invitations: SpaceInvitation[]
  pendingRevokeId?: string | null
  onRevoke: (invitation: SpaceInvitation) => void
}

export function InvitationList({ invitations, pendingRevokeId, onRevoke }: InvitationListProps) {
  const { t, i18n } = useTranslation('spaces')

  if (invitations.length === 0) {
    return <p className="text-sm text-fg-2">{t('invitations.empty')}</p>
  }

  return (
    <ul className="divide-y divide-bg-3 rounded-2xl border border-border bg-bg-1 overflow-hidden">
      {invitations.map((invitation) => (
        <InvitationRow
          key={invitation.id}
          invitation={invitation}
          revoking={pendingRevokeId === invitation.id}
          onRevoke={onRevoke}
          lang={i18n.language}
        />
      ))}
    </ul>
  )
}

interface RowProps {
  invitation: SpaceInvitation
  revoking: boolean
  onRevoke: (invitation: SpaceInvitation) => void
  lang: string
}

function InvitationRow({ invitation, revoking, onRevoke, lang }: RowProps) {
  const { t } = useTranslation('spaces')
  const [copied, setCopied] = useState(false)

  async function handleCopy() {
    try {
      await navigator.clipboard.writeText(invitation.code)
      setCopied(true)
      setTimeout(() => setCopied(false), 1500)
    } catch {
      // Clipboard access denied (permissions, insecure context): the code
      // stays visible on the row for a manual copy.
    }
  }

  return (
    <li className="flex flex-wrap items-center gap-3 px-[18px] py-3.5">
      <div className="min-w-0 flex-1">
        <div className="text-[14.5px] font-semibold text-fg-0 truncate">{invitation.email}</div>
        {invitation.status === 'PENDING' && (
          <div className="text-[12.5px] text-fg-3 truncate">
            {t('invitations.expires', { time: formatRelativeTime(invitation.expiresAt, lang) })}
          </div>
        )}
      </div>

      <SpaceRolePill role={invitation.role} label={t(`space:role.${invitation.role}`)} />

      <span className={`inline-flex items-center px-[9px] py-[3px] rounded-[6px] text-[11px] font-bold tracking-[0.02em] whitespace-nowrap ${STATUS_CLASS[invitation.status]}`}>
        {t(`invitations.status.${invitation.status}`)}
      </span>

      <button
        type="button"
        onClick={() => { void handleCopy() }}
        aria-label={t('invitations.action_copy', { code: invitation.code })}
        className="flex items-center gap-1.5 rounded-[8px] border border-border bg-bg-1 px-2.5 py-1.5 font-mono text-xs text-fg-1 transition-colors hover:bg-bg-2"
      >
        {copied ? <Check className="size-3.5 text-status-green" aria-hidden="true" /> : <Copy className="size-3.5" aria-hidden="true" />}
        {invitation.code}
      </button>

      {invitation.status === 'PENDING' && (
        <button
          type="button"
          aria-label={t('invitations.action_revoke', { email: invitation.email })}
          disabled={revoking}
          onClick={() => onRevoke(invitation)}
          className="flex size-8 items-center justify-center rounded-md text-fg-2 transition-colors hover:bg-bg-2 hover:text-status-red disabled:opacity-50"
        >
          <X className="size-4" />
        </button>
      )}
    </li>
  )
}
