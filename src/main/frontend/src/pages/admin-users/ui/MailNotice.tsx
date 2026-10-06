import { useTranslation } from 'react-i18next'
import { Mail } from 'lucide-react'
import type { AdminUser } from '@/entities/user'
import type { MailAvailability } from '@/entities/capabilities'

interface MailNoticeProps {
  user: AdminUser
  mail: MailAvailability
  /** Deleting an invited account is the one gesture its holder still hears about: their invitation is cancelled. */
  isDeletion?: boolean
}

/** Says, before an administrator confirms, whether the account's holder will be told by mail. */
export function MailNotice({ user, mail, isDeletion = false }: MailNoticeProps) {
  const { t } = useTranslation('adminUsers')
  if (mail === 'loading') return null
  const key = mail === 'unavailable'
    ? 'notice.no_mail'
    : user.invitation
      ? (isDeletion ? 'notice.invitation_cancelled' : 'notice.invited')
      : 'notice.mail'
  return (
    <p className="mb-4 flex items-start gap-2 text-[13px] leading-relaxed text-fg-2">
      <Mail className="mt-0.5 size-3.5 shrink-0" aria-hidden="true" />
      <span>{t(key, { username: user.username })}</span>
    </p>
  )
}
