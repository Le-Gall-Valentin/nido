import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Check, Copy } from 'lucide-react'
import { Alert, Button } from '@/shared/ui'
import { copyText } from '@/shared/lib'
import type { InvitationDelivery } from '@/entities/user'

interface InvitationResultProps {
  username: string
  email: string
  delivery: InvitationDelivery
}

/**
 * How an invitation left: by mail, or as a link for the administrator to pass on. The link is shown this once
 * — only its hash is stored — and says so.
 */
export function InvitationResult({ username, email, delivery }: InvitationResultProps) {
  const { t } = useTranslation('adminUsers')
  const [copied, setCopied] = useState(false)

  if (delivery.delivery === 'mail') {
    return <p className="text-sm leading-relaxed text-fg-2">{t('invitation.mailed', { username, email })}</p>
  }

  // Only a path when the installation does not know its public address: this page's address stands in.
  const link = new URL(delivery.link, window.location.origin).toString()

  async function handleCopy() {
    // Not copied: the link stays in its field, selected on focus, for a manual copy.
    if (await copyText(link)) {
      setCopied(true)
      setTimeout(() => setCopied(false), 1500)
    }
  }

  return (
    <div>
      <p className="mb-3 text-sm leading-relaxed text-fg-2">{t('invitation.link_intro', { username })}</p>
      <div className="flex flex-col gap-2 sm:flex-row">
        <input
          type="text"
          readOnly
          value={link}
          aria-label={t('invitation.link_label')}
          onFocus={(e) => e.currentTarget.select()}
          className="min-w-0 flex-1 rounded-[10px] border-[1.5px] border-border bg-bg-2 px-3 py-2.5 font-mono text-xs text-fg-0"
        />
        <Button type="button" onClick={() => { void handleCopy() }} className="shrink-0">
          {copied
            ? <Check className="size-4 text-status-green" aria-hidden="true" />
            : <Copy className="size-4" aria-hidden="true" />}
          {copied ? t('invitation.copied') : t('invitation.copy')}
        </Button>
      </div>
      <Alert variant="warning" className="mt-3">{t('invitation.once')}</Alert>
    </div>
  )
}
