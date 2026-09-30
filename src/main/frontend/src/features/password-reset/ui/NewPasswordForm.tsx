import React, { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Eye, EyeOff } from 'lucide-react'
import { Alert, Button, Input, CTA_BUTTON_SHADOW, CTA_BUTTON_STYLE } from '@/shared/ui'
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

  const toggle = (
    <button
      type="button"
      onClick={() => setShowPassword((v) => !v)}
      className="rounded-md p-2 text-fg-3 transition-colors hover:bg-bg-2 hover:text-fg-0"
      aria-label={showPassword ? t('field.hide_password') : t('field.show_password')}
    >
      {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
    </button>
  )

  return (
    <form onSubmit={(e) => void handleSubmit(e)} aria-labelledby={labelId} className="flex flex-col gap-4">
      {error && <Alert variant="error">{t(error.key, error.seconds === undefined ? undefined : { seconds: error.seconds })}</Alert>}
      <div className="flex flex-col gap-1.5">
        <Input
          label={t('field.new_password')}
          name="newPassword"
          type={showPassword ? 'text' : 'password'}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete="new-password"
          spellCheck={false}
          autoCapitalize="off"
          autoFocus
          className="rounded-[11px] px-[15px] py-[13px] text-[15px]"
          suffix={toggle}
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
        className="rounded-[11px] px-[15px] py-[13px] text-[15px]"
        aria-invalid={mismatch}
      />
      {mismatch && <p className="-mt-2 text-xs text-status-orange">{t('error.mismatch')}</p>}
      <Button
        type="submit"
        disabled={!canSave}
        isLoading={isLoading}
        className="mt-2 w-full rounded-[11px] border-transparent py-3.5 text-[15px] font-semibold active:translate-y-px disabled:cursor-not-allowed"
        style={{ ...CTA_BUTTON_STYLE, boxShadow: CTA_BUTTON_SHADOW }}
      >
        {t('action.save')}
      </Button>
    </form>
  )
}
