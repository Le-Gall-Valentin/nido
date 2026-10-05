import React, { useId, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, Button, Input, PasswordInput, AUTH_FIELD_CLASS, AUTH_SUBMIT_CLASS, CTA_ELEVATED_STYLE, VERBATIM_INPUT_PROPS } from '@/shared/ui'
import { isValidPassword, passwordProblem } from '@/shared/lib'
import { InvalidResetLinkError } from '../model/errors'
import { describeError, type FormError } from '../model/describeError'

interface Props {
  /** Saves the password with the link the page holds. @throws InvalidResetLinkError, WeakPasswordError */
  onSave: (password: string) => Promise<void>
  /** The account's username: handed to password managers, so they store the password under the right name. */
  username?: string
  labelId?: string
  onDone: () => void
  /** The link stopped working between opening the page and saving (expired, or used elsewhere). */
  onInvalid: () => void
}

export function NewPasswordForm({ onSave, username, labelId, onDone, onInvalid }: Props) {
  const { t } = useTranslation('passwordReset')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState<FormError | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const isSubmittingRef = useRef(false)
  const rulesId = useId()
  const tooLongId = useId()
  const mismatchId = useId()
  const errorId = useId()

  const mismatch = confirm.length > 0 && password !== confirm
  // The rules text says 72 characters; past 72 bytes it needs saying why a password that fits is refused.
  const tooLong = passwordProblem(password) === 'too_long'
  const canSave = isValidPassword(password) && password === confirm
  // What the server refused, said on the field it is about rather than only at the top of the form.
  const refusedByServer = error?.key === 'error.weak'
  const passwordDescription = [rulesId, tooLong ? tooLongId : null, refusedByServer ? errorId : null]
    .filter(Boolean).join(' ')

  async function handleSubmit(e: React.SyntheticEvent<HTMLFormElement>): Promise<void> {
    e.preventDefault()
    if (!canSave || isSubmittingRef.current) return
    isSubmittingRef.current = true
    setIsLoading(true)
    setError(null)
    try {
      await onSave(password)
      onDone()
    } catch (err) {
      if (err instanceof InvalidResetLinkError) {
        onInvalid()
        return
      }
      setError(describeError(err))
    } finally {
      isSubmittingRef.current = false
      setIsLoading(false)
    }
  }

  return (
    <form onSubmit={(e) => void handleSubmit(e)} aria-labelledby={labelId} className="flex flex-col gap-4">
      {username && <input type="text" name="username" autoComplete="username" value={username} readOnly hidden />}
      {error && (
        <div id={errorId}>
          <Alert variant="error">{t(error.key, error.seconds === undefined ? undefined : { seconds: error.seconds })}</Alert>
        </div>
      )}
      <div className="flex flex-col gap-1.5">
        <PasswordInput
          label={t('field.new_password')}
          name="newPassword"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete="new-password"
          autoFocus
          className={AUTH_FIELD_CLASS}
          visible={showPassword}
          onVisibleChange={setShowPassword}
          aria-invalid={tooLong || refusedByServer}
          aria-describedby={passwordDescription}
        />
        <p id={rulesId} className="text-xs leading-relaxed text-fg-2">{t('rules')}</p>
        {tooLong && <p id={tooLongId} className="text-xs text-status-orange">{t('error.too_long')}</p>}
      </div>
      <Input
        label={t('field.confirm')}
        name="confirmPassword"
        type={showPassword ? 'text' : 'password'}
        value={confirm}
        onChange={(e) => setConfirm(e.target.value)}
        autoComplete="new-password"
        {...VERBATIM_INPUT_PROPS}
        className={AUTH_FIELD_CLASS}
        aria-invalid={mismatch}
        aria-describedby={mismatch ? mismatchId : undefined}
      />
      {mismatch && <p id={mismatchId} className="-mt-2 text-xs text-status-orange">{t('error.mismatch')}</p>}
      <Button
        type="submit"
        disabled={!canSave}
        isLoading={isLoading}
        className={`${AUTH_SUBMIT_CLASS} disabled:cursor-not-allowed`}
        style={CTA_ELEVATED_STYLE}
      >
        {t('action.save')}
      </Button>
    </form>
  )
}
