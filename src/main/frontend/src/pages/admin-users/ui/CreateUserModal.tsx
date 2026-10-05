import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, Dialog, Button, Input, CTA_BUTTON_STYLE, VERBATIM_INPUT_PROPS } from '@/shared/ui'
import { isValidEmail, isValidUsername, usernameProblem } from '@/shared/lib'
import type { InvitationDelivery, User } from '@/entities/user'
import { assignableRoles } from '../lib/permissions'
import { mapApiErrorToKey } from '../lib/mapApiErrorToKey'
import { InvitationResult } from './InvitationResult'

interface CreateUserModalProps {
  caller: User
  onClose: () => void
  onCreate: (username: string, email: string, role: 'USER' | 'ADMIN') => Promise<InvitationDelivery>
  /** Called when the dialog closes after the account was created. */
  onSuccess: () => void
}

export function CreateUserModal({ caller, onClose, onCreate, onSuccess }: CreateUserModalProps) {
  const { t } = useTranslation('adminUsers')
  const roles = assignableRoles(caller.role)

  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [role, setRole] = useState<'USER' | 'ADMIN'>('USER')
  const [isLoading, setIsLoading] = useState(false)
  const [errorKey, setErrorKey] = useState<string | null>(null)
  const [created, setCreated] = useState<{ username: string; email: string; delivery: InvitationDelivery } | null>(null)
  const pendingRef = useRef(false)

  const trimmedUsername = username.trim()
  const trimmedEmail = email.trim()

  // Mirror of the backend RegisterRequest constraints.
  const usernameIssue = trimmedUsername.length > 0 ? usernameProblem(trimmedUsername) : null
  const emailInvalid = trimmedEmail.length > 0 && !isValidEmail(trimmedEmail)
  const canSubmit = isValidUsername(trimmedUsername) && isValidEmail(trimmedEmail)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!canSubmit || pendingRef.current) return
    pendingRef.current = true
    setIsLoading(true)
    setErrorKey(null)
    try {
      const delivery = await onCreate(trimmedUsername, trimmedEmail, role)
      setCreated({ username: trimmedUsername, email: trimmedEmail, delivery })
    } catch (error) {
      setErrorKey(mapApiErrorToKey(error, 'create'))
    } finally {
      pendingRef.current = false
      setIsLoading(false)
    }
  }

  function handleClose() {
    setErrorKey(null)
    if (created) {
      onSuccess()
      return
    }
    onClose()
  }

  if (created) {
    return (
      <Dialog open onClose={handleClose} title={t('invitation.created_title')} maxWidth="max-w-lg">
        <h3 className="mb-4 text-xl font-semibold text-fg-0">{t('invitation.created_title')}</h3>
        <InvitationResult username={created.username} email={created.email} delivery={created.delivery} />
        <div className="mt-5 flex justify-end">
          <Button type="button" onClick={handleClose} className="border-transparent font-semibold" style={CTA_BUTTON_STYLE}>
            {t('invitation.done')}
          </Button>
        </div>
      </Dialog>
    )
  }

  return (
    <Dialog open onClose={handleClose} title={t('create.title')} maxWidth="max-w-lg">
      <div className="mb-5">
        <h3 className="text-xl font-semibold text-fg-0">{t('create.title')}</h3>
        <p className="mt-1.5 text-sm text-fg-2">{t('create.intro')}</p>
      </div>
      <form onSubmit={(e) => void handleSubmit(e)}>
        <div className="flex flex-col gap-3 sm:flex-row mb-3">
          <div className="min-w-0 flex-1">
            <Input
              label={t('create.username')}
              name="username"
              value={username}
              onChange={e => setUsername(e.target.value)}
              placeholder={t('create.username_placeholder')}
              disabled={isLoading}
              autoFocus
              {...VERBATIM_INPUT_PROPS}
            />
          </div>
          <div className="min-w-0 flex-1">
            <Input
              label={t('create.email')}
              name="email"
              type="email"
              value={email}
              onChange={e => setEmail(e.target.value)}
              placeholder={t('create.email_placeholder')}
              disabled={isLoading}
            />
          </div>
        </div>

        <div aria-live="polite">
          {(usernameIssue === 'too_short' || usernameIssue === 'too_long') && <p className="text-xs text-status-orange mb-2">{t('create.error.username_length')}</p>}
          {usernameIssue === 'has_at' && <p className="text-xs text-status-orange mb-2">{t('create.error.username_at')}</p>}
          {emailInvalid && <p className="text-xs text-status-orange mb-2">{t('create.error.email_invalid')}</p>}
        </div>

        <div className="mb-1 flex flex-col gap-2">
          <label htmlFor="create-role" className="text-[13px] font-semibold text-fg-1">
            {t('create.role')}
          </label>
          <select
            id="create-role"
            value={role}
            onChange={e => setRole(e.target.value as 'USER' | 'ADMIN')}
            disabled={isLoading || roles.length === 1}
            className="w-full rounded-[10px] border-[1.5px] border-border bg-bg-1 px-3 py-[11px] text-sm text-fg-0 outline-none transition-all hover:border-border-2 focus:border-accent focus:shadow-[0_0_0_3px_var(--color-accent-ring)] disabled:opacity-50"
          >
            {roles.map(r => (
              <option key={r} value={r}>{t(`user.role.${r}`, { ns: 'shell' })}</option>
            ))}
          </select>
          <p className="text-[11px] text-fg-3">{t('create.role_note')}</p>
        </div>

        {errorKey && (
          <Alert variant="error" className="mt-3">{t(errorKey)}</Alert>
        )}

        <div className="flex justify-end gap-2 mt-5">
          <Button type="button" onClick={handleClose} disabled={isLoading}>
            {t('create.cancel')}
          </Button>
          <Button
            type="submit"
            disabled={!canSubmit}
            isLoading={isLoading}
            className="border-transparent font-semibold"
            style={CTA_BUTTON_STYLE}
          >
            {t('create.submit')}
          </Button>
        </div>
      </form>
    </Dialog>
  )
}
