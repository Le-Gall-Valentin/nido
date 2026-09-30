import React, { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, Button, Input, PasswordInput, AUTH_FIELD_CLASS, AUTH_SUBMIT_CLASS, CTA_ELEVATED_STYLE } from '@/shared/ui'
import { isValidPassword, passwordProblem } from '@/shared/lib'
import type { IPasswordResetApi } from '../model/IPasswordResetApi'
import { InvalidResetLinkError } from '../model/errors'
import { describeError, type FormError } from '../model/describeError'

interface Props {
  api: IPasswordResetApi
  token: string
  labelId?: string
  onDone: () => void
  /** The link stopped working between opening the page and saving (expired, or used elsewhere). */
  onInvalid: () => void
}

export function NewPasswordForm({ api, token, labelId, onDone, onInvalid }: Props) {
  const { t } = useTranslation('passwordReset')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState<FormError | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const isSubmittingRef = useRef(false)

  const mismatch = confirm.length > 0 && password !== confirm
  // The rules text says 72 characters; past 72 bytes it needs saying why a password that fits is refused.
  const tooLong = passwordProblem(password) === 'too_long'
  const canSave = isValidPassword(password) && password === confirm

  async function handleSubmit(e: React.SyntheticEvent<HTMLFormElement>): Promise<void> {
    e.preventDefault()
    if (!canSave || isSubmittingRef.current) return
    isSubmittingRef.current = true
    setIsLoading(true)
    setError(null)
    try {
      await api.confirmReset(token, password)
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
      {error && <Alert variant="error">{t(error.key, error.seconds === undefined ? undefined : { seconds: error.seconds })}</Alert>}
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
        />
        <p className="text-xs leading-relaxed text-fg-2">{t('rules')}</p>
        {tooLong && <p className="text-xs text-status-orange">{t('error.too_long')}</p>}
      </div>
      <Input
        label={t('field.confirm')}
        name="confirmPassword"
        type={showPassword ? 'text' : 'password'}
        value={confirm}
        onChange={(e) => setConfirm(e.target.value)}
        autoComplete="new-password"
        spellCheck={false}
        autoCapitalize="off"
        className={AUTH_FIELD_CLASS}
        aria-invalid={mismatch}
      />
      {mismatch && <p className="-mt-2 text-xs text-status-orange">{t('error.mismatch')}</p>}
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
