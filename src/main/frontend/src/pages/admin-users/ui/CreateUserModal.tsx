import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, Dialog, Button, Input, CTA_BUTTON_STYLE } from '@/shared/ui'
import { isValidPassword, isValidEmail, PASSWORD_MIN_LENGTH, PASSWORD_MAX_LENGTH } from '@/shared/lib'
import type { User } from '@/entities/user'
import { assignableRoles } from '../lib/permissions'
import { mapApiErrorToKey } from '../lib/mapApiErrorToKey'

interface CreateUserModalProps {
  caller: User
  onClose: () => void
  onCreate: (username: string, email: string, password: string, role: 'USER' | 'ADMIN') => Promise<void>
  onSuccess: () => void
}

export function CreateUserModal({ caller, onClose, onCreate, onSuccess }: CreateUserModalProps) {
  const { t } = useTranslation('adminUsers')
  const roles = assignableRoles(caller.role)

  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState<'USER' | 'ADMIN'>('USER')
  const [isLoading, setIsLoading] = useState(false)
  const [errorKey, setErrorKey] = useState<string | null>(null)
  const pendingRef = useRef(false)

  const trimmedUsername = username.trim()
  const trimmedEmail = email.trim()

  // Mirror of the backend RegisterRequest constraints.
  const usernameInvalid = trimmedUsername.length > 0 && (trimmedUsername.length < 3 || trimmedUsername.length > 50)
  const emailInvalid = trimmedEmail.length > 0 && !isValidEmail(trimmedEmail)
  const passwordTooShort = password.length > 0 && password.length < PASSWORD_MIN_LENGTH
  const passwordTooLong = password.length > PASSWORD_MAX_LENGTH
  const passwordWeak =
    password.length >= PASSWORD_MIN_LENGTH &&
    password.length <= PASSWORD_MAX_LENGTH &&
    !isValidPassword(password)
  const canSubmit =
    trimmedUsername.length >= 3 &&
    trimmedUsername.length <= 50 &&
    isValidEmail(trimmedEmail) &&
    isValidPassword(password)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!canSubmit || pendingRef.current) return
    pendingRef.current = true
    setIsLoading(true)
    setErrorKey(null)
    try {
      await onCreate(trimmedUsername, trimmedEmail, password, role)
      onSuccess()
    } catch (error) {
      setErrorKey(mapApiErrorToKey(error, 'create'))
    } finally {
      pendingRef.current = false
      setIsLoading(false)
    }
  }

  function handleClose() {
    setErrorKey(null)
    onClose()
  }

  return (
    <Dialog open onClose={handleClose} title={t('create.title')} maxWidth="max-w-lg">
      <div className="mb-5">
        <h3 className="text-base font-semibold text-fg-0">{t('create.title')}</h3>
      </div>
      <form onSubmit={handleSubmit}>
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

        <div className="mb-3">
          <Input
            label={t('create.password')}
            name="password"
            type="password"
            autoComplete="new-password"
            value={password}
            onChange={e => setPassword(e.target.value)}
            disabled={isLoading}
          />
        </div>

        <div aria-live="polite">
          {usernameInvalid && <p className="text-xs text-status-orange mb-2">{t('create.error.username_length')}</p>}
          {emailInvalid && <p className="text-xs text-status-orange mb-2">{t('create.error.email_invalid')}</p>}
          {passwordTooShort && <p className="text-xs text-status-orange mb-2">{t('create.error.password_too_short')}</p>}
          {passwordTooLong && <p className="text-xs text-status-orange mb-2">{t('create.error.password_too_long')}</p>}
          {passwordWeak && <p className="text-xs text-status-orange mb-2">{t('create.error.password_weak')}</p>}
        </div>

        <div className="mb-1 flex flex-col gap-2">
          <label htmlFor="create-role" className="text-xs font-medium text-fg-1">
            {t('create.role')}
          </label>
          <select
            id="create-role"
            value={role}
            onChange={e => setRole(e.target.value as 'USER' | 'ADMIN')}
            disabled={isLoading || roles.length === 1}
            className="w-full rounded-lg border border-border bg-bg-1 px-3.5 py-3 text-sm text-fg-0 outline-none transition-all hover:border-border-2 focus:border-accent focus:bg-bg-2 focus:shadow-[0_0_0_3px_var(--color-accent-ring)] disabled:opacity-50"
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
