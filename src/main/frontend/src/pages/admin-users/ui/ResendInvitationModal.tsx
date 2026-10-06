import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Send } from 'lucide-react'
import { Alert, Button, Dialog, CTA_BUTTON_STYLE } from '@/shared/ui'
import type { AdminUser, InvitationDelivery } from '@/entities/user'
import type { MailAvailability } from '@/entities/capabilities'
import { useDialogSubmit } from '../lib/useDialogSubmit'
import { InvitationResult } from './InvitationResult'

interface ResendInvitationModalProps {
  user: AdminUser
  /** Only the wording depends on it: the server decides how the invitation leaves. */
  mail: MailAvailability
  onClose: () => void
  onResend: (id: string) => Promise<InvitationDelivery>
}

export function ResendInvitationModal({ user, mail, onClose, onResend }: ResendInvitationModalProps) {
  const { t } = useTranslation('adminUsers')
  const byMail = mail === 'available'
  const [delivery, setDelivery] = useState<InvitationDelivery | null>(null)
  const { submit, isLoading, errorKey } = useDialogSubmit('resend')

  function handleSubmit() {
    void submit(async () => {
      setDelivery(await onResend(user.id))
    })
  }

  if (delivery) {
    return (
      <Dialog open onClose={onClose} title={t('resend.done_title')} maxWidth="max-w-lg">
        <h3 className="mb-4 text-xl font-semibold text-fg-0">{t('resend.done_title')}</h3>
        <InvitationResult username={user.username} email={user.email} delivery={delivery} />
        <div className="mt-5 flex justify-end">
          <Button type="button" onClick={onClose} className="border-transparent font-semibold" style={CTA_BUTTON_STYLE}>
            {t('invitation.done')}
          </Button>
        </div>
      </Dialog>
    )
  }

  const title = t(byMail ? 'resend.title_mail' : 'resend.title_link', { username: user.username })
  return (
    <Dialog open onClose={onClose} title={title} maxWidth="max-w-[460px]">
      <div className="mb-5">
        <h3 className="mb-1.5 text-xl font-semibold text-fg-0">{title}</h3>
        <p className="text-sm leading-relaxed text-fg-2">{t(byMail ? 'resend.body_mail' : 'resend.body_link')}</p>
      </div>
      {errorKey && <Alert variant="error" className="mb-4">{t(errorKey)}</Alert>}
      <div className="flex justify-end gap-2">
        <Button type="button" onClick={onClose} disabled={isLoading}>{t('resend.cancel')}</Button>
        <Button
          onClick={() => { void handleSubmit() }}
          isLoading={isLoading}
          className="border-transparent font-semibold"
          style={CTA_BUTTON_STYLE}
        >
          <Send className="size-4" />
          {t(byMail ? 'resend.submit_mail' : 'resend.submit_link')}
        </Button>
      </div>
    </Dialog>
  )
}
