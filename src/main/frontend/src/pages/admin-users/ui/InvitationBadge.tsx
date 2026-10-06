import { useTranslation } from 'react-i18next'
import type { InvitationState } from '@/entities/user'

/** An account that has not chosen its password yet: its invitation still usable, or to be sent again. */
export function InvitationBadge({ invitation }: { invitation: InvitationState }) {
  const { t } = useTranslation('adminUsers')
  const expired = invitation.status === 'expired'
  return (
    <span className={`inline-flex items-center px-[9px] py-[3px] rounded-[6px] text-[11px] font-semibold ${
      expired ? 'bg-status-red-dim text-status-red' : 'bg-status-orange-dim text-status-orange'}`}>
      {t(expired ? 'invitation.expired' : 'invitation.pending')}
    </span>
  )
}
