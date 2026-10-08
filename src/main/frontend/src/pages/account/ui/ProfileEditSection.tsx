import { useId, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, Button, Input, CTA_BUTTON_STYLE, VERBATIM_INPUT_PROPS } from '@/shared/ui'
import type { User } from '@/entities/user'
import { ResendTooSoonError, SendLimitError, commonErrorMessage } from '@/features/two-factor'
import { ConflictError, InvalidCurrentPasswordError } from '../api/accountApi'
import type { ProfileUpdateResult } from '../model/IAccountApi'
import { useFlash } from '../model/useFlash'
import { EmailCodeDialog } from './EmailCodeDialog'
import { NetworkError, RateLimitError, isValidUsername, usernameProblem, type UsernameProblem } from '@/shared/lib'


interface ProfileEditSectionProps {
  user: User
  onPatch: (partial: Partial<User>) => void
  onUpdateProfile: (username: string, email: string, currentPassword?: string, emailCode?: string) => Promise<ProfileUpdateResult>
}


const PROFILE_USERNAME_ERRORS = {
  too_short: 'profile.error.username_too_short',
  too_long: 'profile.error.username_too_long',
  has_at: 'profile.error.username_at',
} as const satisfies Record<UsernameProblem, string>

export function ProfileEditSection({ user, onPatch, onUpdateProfile }: ProfileEditSectionProps) {
  const { t } = useTranslation('account')
  const [username, setUsername] = useState(user.username)
  const [email, setEmail] = useState(user.email)
  const [currentPassword, setCurrentPassword] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [emailCode, setEmailCode] = useState<{ sentTo: string; resendAfterSeconds: number } | null>(null)
  const { flash, showFlash, clearFlash } = useFlash(3000)
  // Stays until the next save: what the person needs to act on must not fade out after three seconds.
  const [mailMethodRemoved, setMailMethodRemoved] = useState(false)

  const trimmedUsername = username.trim()
  const trimmedEmail = email.trim()
  const isDirty = trimmedUsername !== user.username || trimmedEmail !== user.email
  // Nothing is said about an empty field, but it cannot be saved either.
  const usernameIssue = trimmedUsername.length > 0 ? usernameProblem(trimmedUsername) : null
  const emailEmpty = trimmedEmail.length === 0
  // The address is how an account is recovered, so changing it asks for the password. A change of
  // letter case only is the same mailbox — the server agrees and asks for nothing.
  const changesAddress = (value: string) => value.trim().toLowerCase() !== user.email.toLowerCase()
  const addressChanges = changesAddress(email)
  const passwordHintId = useId()
  const canSave = isDirty && isValidUsername(trimmedUsername) && !emailEmpty
    && (!addressChanges || currentPassword.length > 0)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!canSave || isSubmitting) return
    setIsSubmitting(true)
    setMailMethodRemoved(false)
    try {
      const result = await onUpdateProfile(trimmedUsername, trimmedEmail, addressChanges ? currentPassword : undefined, undefined)
      if (result.kind === 'email_code_sent') {
        // Nothing is saved yet: the same change comes back with the code. The password stays in the form for it.
        setEmailCode({ sentTo: result.sentTo, resendAfterSeconds: result.resendAfterSeconds })
        return
      }
      saved(result.mailMethodRemoved)
    } catch (error) {
      if (error instanceof ResendTooSoonError) {
        // Saved again within the minute (after cancelling the code, say): the code already sent still works.
        setEmailCode({ sentTo: trimmedEmail, resendAfterSeconds: error.seconds })
      } else if (error instanceof SendLimitError) {
        const message = commonErrorMessage(error)
        showFlash('error', message.key, message.values)
      } else if (error instanceof InvalidCurrentPasswordError) {
        setCurrentPassword('')
        showFlash('error', 'profile.error.wrong_password')
      } else if (error instanceof ConflictError) {
        showFlash('error', 'profile.error.conflict')
      } else if (error instanceof RateLimitError) {
        showFlash('error', 'profile.error.rate_limit')
      } else if (error instanceof NetworkError) {
        showFlash('error', 'profile.error.network')
      } else {
        showFlash('error', 'profile.error.server')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  /** @param mailMethodRemoved saved while mail was off: nothing proved the new address, so the code by mail went */
  function saved(mailMethodRemoved: boolean) {
    onPatch(mailMethodRemoved
      ? { username: trimmedUsername, email: trimmedEmail, twoFactorMethods: user.twoFactorMethods.filter(m => m !== 'MAIL') }
      : { username: trimmedUsername, email: trimmedEmail })
    setCurrentPassword('')
    setEmailCode(null)
    if (mailMethodRemoved) {
      clearFlash()
      setMailMethodRemoved(true)
    } else {
      showFlash('success', 'profile.success')
    }
  }

  async function confirmEmailCode(code: string) {
    const result = await onUpdateProfile(trimmedUsername, trimmedEmail, currentPassword, code)
    if (result.kind === 'saved') saved(result.mailMethodRemoved)
  }

  // Mail may have gone off since the first code: the same change then saves at once, and the dialog has no code to wait for.
  async function resendEmailCode(): Promise<number> {
    const result = await onUpdateProfile(trimmedUsername, trimmedEmail, currentPassword, undefined)
    if (result.kind === 'saved') {
      saved(result.mailMethodRemoved)
      return 0
    }
    return result.resendAfterSeconds
  }

  function handleCancel() {
    setUsername(user.username)
    setEmail(user.email)
    setCurrentPassword('')
    clearFlash()
  }

  return (
    <section id="section-profile" className="rounded-2xl border border-border bg-bg-1 mb-4 overflow-hidden">
      <div className="px-7 pt-6">
        <h3 className="text-lg font-semibold text-fg-0">{t('profile.title')}</h3>
        <p className="text-[13.5px] text-fg-2 mt-0.5">{t('profile.subtitle')}</p>
      </div>
      <div className="px-7 py-5">
        <form onSubmit={(e) => void handleSubmit(e)}>
          {usernameIssue && <p className="text-xs text-status-orange mb-2">{t(PROFILE_USERNAME_ERRORS[usernameIssue])}</p>}
          {emailEmpty && isDirty && <p className="text-xs text-status-orange mb-2">{t('profile.error.email_required')}</p>}
          <div className="flex flex-col gap-3 sm:flex-row mb-3">
            <div className="min-w-0 sm:flex-1">
              <Input
                label={t('profile.username')}
                name="username"
                value={username}
                onChange={e => setUsername(e.target.value)}
                disabled={isSubmitting}
                {...VERBATIM_INPUT_PROPS}
              />
            </div>
            <div className="min-w-0 sm:flex-[1.4]">
              <Input
                label={t('profile.email')}
                name="email"
                type="email"
                value={email}
                onChange={e => {
                  setEmail(e.target.value)
                  // Back to the address it had, the password is no longer asked: nothing typed for it stays.
                  if (!changesAddress(e.target.value)) setCurrentPassword('')
                }}
                disabled={isSubmitting}
              />
            </div>
          </div>
          {addressChanges && !emailEmpty && (
            <div className="mb-3 flex flex-col gap-1.5">
              <Input
                label={t('profile.current_password')}
                name="currentPassword"
                type="password"
                value={currentPassword}
                onChange={e => setCurrentPassword(e.target.value)}
                autoComplete="current-password"
                disabled={isSubmitting}
                aria-describedby={passwordHintId}
              />
              <p id={passwordHintId} className="text-xs text-fg-2">{t('profile.current_password_hint')}</p>
            </div>
          )}
          {mailMethodRemoved && <Alert variant="warning" className="mb-3">{t('profile.success_mail_removed')}</Alert>}
          {flash && (
            <div
              role={flash.kind === 'success' ? 'status' : 'alert'}
              className={`mb-3 text-xs px-3 py-2 rounded-lg ${flash.kind === 'success' ? 'bg-status-green-dim text-status-green' : 'bg-status-red-dim text-status-red'}`}
            >
              {t(flash.key, flash.values)}
            </div>
          )}
          <div className="flex justify-end gap-2 mt-1">
            <Button type="button" onClick={handleCancel} disabled={!isDirty || isSubmitting} className="border-transparent bg-transparent hover:bg-bg-2 hover:border-transparent">
              {t('profile.cancel')}
            </Button>
            <Button type="submit" disabled={!canSave} isLoading={isSubmitting} className="border-transparent font-semibold" style={CTA_BUTTON_STYLE}>
              {t('profile.save')}
            </Button>
          </div>
        </form>
      </div>
      {emailCode && (
        <EmailCodeDialog
          sentTo={emailCode.sentTo}
          previousAddress={user.email}
          resendAfterSeconds={emailCode.resendAfterSeconds}
          onConfirm={confirmEmailCode}
          onResend={resendEmailCode}
          onCancel={() => setEmailCode(null)}
        />
      )}
    </section>
  )
}