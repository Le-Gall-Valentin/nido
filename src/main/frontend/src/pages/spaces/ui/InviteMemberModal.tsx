import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Check, Copy, Send } from 'lucide-react'
import { Alert, Dialog, Button, Input, CTA_BUTTON_STYLE } from '@/shared/ui'
import { isValidEmail } from '@/shared/lib'
import type { SpaceInvitation } from '@/entities/space'
import type { AssignableSpaceRole } from '../model/ISpacesPageApi'
import { mapSpaceErrorToKey } from '../lib/mapSpaceErrorToKey'

const ASSIGNABLE_ROLES: AssignableSpaceRole[] = ['ADMIN', 'MEMBER', 'VIEWER']

interface InviteMemberModalProps {
  onClose: () => void
  onInvite: (email: string, role: AssignableSpaceRole) => Promise<SpaceInvitation>
  onSuccess: () => void
}

export function InviteMemberModal({ onClose, onInvite, onSuccess }: InviteMemberModalProps) {
  const { t } = useTranslation('spaces')

  const [email, setEmail] = useState('')
  const [role, setRole] = useState<AssignableSpaceRole>('MEMBER')
  const [isLoading, setIsLoading] = useState(false)
  const [errorKey, setErrorKey] = useState<string[] | null>(null)
  const [issued, setIssued] = useState<SpaceInvitation | null>(null)
  const [copied, setCopied] = useState(false)
  const pendingRef = useRef(false)

  const trimmedEmail = email.trim()
  const canSubmit = isValidEmail(trimmedEmail)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!canSubmit || pendingRef.current) return
    pendingRef.current = true
    setIsLoading(true)
    setErrorKey(null)
    try {
      const invitation = await onInvite(trimmedEmail, role)
      setIssued(invitation)
    } catch (error) {
      setErrorKey(mapSpaceErrorToKey(error, 'invite'))
    } finally {
      pendingRef.current = false
      setIsLoading(false)
    }
  }

  async function handleCopy() {
    if (!issued) return
    try {
      await navigator.clipboard.writeText(issued.code)
      setCopied(true)
      setTimeout(() => setCopied(false), 1500)
    } catch {
      // Clipboard access denied: the code stays visible for a manual copy.
    }
  }

  function handleClose() {
    setErrorKey(null)
    if (issued) onSuccess()
    onClose()
  }

  if (issued) {
    return (
      <Dialog open onClose={handleClose} title={t('invite.success_title')} maxWidth="max-w-md">
        <div className="mb-5">
          <h3 className="text-xl font-semibold text-fg-0 mb-1.5">{t('invite.success_title')}</h3>
          <p className="text-sm text-fg-2 leading-relaxed">{t('invite.success_body', { email: issued.email })}</p>
        </div>

        <button
          type="button"
          onClick={() => { void handleCopy() }}
          className="mb-5 flex w-full items-center justify-center gap-2 rounded-[10px] border-[1.5px] border-dashed border-border bg-bg-2 px-3.5 py-3 font-mono text-base font-semibold tracking-wide text-fg-0 transition-colors hover:bg-bg-3"
        >
          {copied ? <Check className="size-4 text-status-green" /> : <Copy className="size-4" />}
          {issued.code}
        </button>

        <div className="flex justify-end">
          <Button
            type="button"
            onClick={handleClose}
            className="border-transparent font-semibold"
            style={CTA_BUTTON_STYLE}
          >
            {t('invite.done')}
          </Button>
        </div>
      </Dialog>
    )
  }

  return (
    <Dialog open onClose={handleClose} title={t('invite.title')} maxWidth="max-w-md">
      <div className="mb-5">
        <h3 className="text-xl font-semibold text-fg-0">{t('invite.title')}</h3>
      </div>
      <form onSubmit={handleSubmit}>
        <div className="mb-3">
          <Input
            label={t('invite.email')}
            name="email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder={t('invite.email_placeholder')}
            disabled={isLoading}
            autoFocus
          />
        </div>

        <div className="mb-1 flex flex-col gap-2">
          <label htmlFor="invite-role" className="text-[13px] font-semibold text-fg-1">
            {t('invite.role')}
          </label>
          <select
            id="invite-role"
            value={role}
            onChange={(e) => setRole(e.target.value as AssignableSpaceRole)}
            disabled={isLoading}
            className="w-full rounded-[10px] border-[1.5px] border-border bg-bg-1 px-3 py-[11px] text-sm text-fg-0 outline-none transition-all hover:border-border-2 focus:border-accent focus:shadow-[0_0_0_3px_var(--color-accent-ring)] disabled:opacity-50"
          >
            {ASSIGNABLE_ROLES.map((r) => (
              <option key={r} value={r}>{t(`role.${r}`)}</option>
            ))}
          </select>
        </div>

        {errorKey && (
          <Alert variant="error" className="mt-3">{t(errorKey)}</Alert>
        )}

        <div className="flex justify-end gap-2 mt-5">
          <Button type="button" onClick={handleClose} disabled={isLoading}>
            {t('invite.cancel')}
          </Button>
          <Button
            type="submit"
            disabled={!canSubmit}
            isLoading={isLoading}
            className="border-transparent font-semibold"
            style={CTA_BUTTON_STYLE}
          >
            <Send className="size-4" />
            {t('invite.submit')}
          </Button>
        </div>
      </form>
    </Dialog>
  )
}
