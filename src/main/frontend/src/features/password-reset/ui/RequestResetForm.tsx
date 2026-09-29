import React, { useId, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, Button, Input, CTA_BUTTON_SHADOW, CTA_BUTTON_STYLE } from '@/shared/ui'
import type { IPasswordResetApi } from '../model/IPasswordResetApi'
import { describeError, type FormError } from '../model/describeError'

interface Props {
  api: IPasswordResetApi
  labelId?: string
  /** Called with the identifier as sent, once the server accepted the request. */
  onSent: (identifier: string) => void
}

export function RequestResetForm({ api, labelId, onSent }: Props) {
  const { t } = useTranslation('passwordReset')
  const errorId = useId()
  const [identifier, setIdentifier] = useState('')
  const [error, setError] = useState<FormError | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const isSubmittingRef = useRef(false)

  async function handleSubmit(e: React.SyntheticEvent<HTMLFormElement>): Promise<void> {
    e.preventDefault()
    const typed = identifier.trim()
    if (!typed || isSubmittingRef.current) return
    isSubmittingRef.current = true
    setIsLoading(true)
    setError(null)
    try {
      await api.requestReset(typed)
      onSent(typed)
    } catch (err) {
      setError(describeError(err))
    } finally {
      isSubmittingRef.current = false
      setIsLoading(false)
    }
  }

  return (
    <form onSubmit={(e) => void handleSubmit(e)} aria-labelledby={labelId} className="flex flex-col gap-4">
      {error && (
        <div id={errorId}>
          <Alert variant="error">{t(error.key, error.seconds === undefined ? undefined : { seconds: error.seconds })}</Alert>
        </div>
      )}
      <Input
        label={t('field.identifier')}
        name="identifier"
        type="text"
        value={identifier}
        onChange={(e) => setIdentifier(e.target.value)}
        placeholder={t('field.identifier_placeholder')}
        autoComplete="username"
        spellCheck={false}
        autoCapitalize="off"
        autoFocus
        className="rounded-[11px] px-[15px] py-[13px] text-[15px]"
        aria-invalid={error !== null}
        aria-describedby={error ? errorId : undefined}
      />
      <Button
        type="submit"
        isLoading={isLoading}
        className="mt-2 w-full rounded-[11px] border-transparent py-3.5 text-[15px] font-semibold active:translate-y-px disabled:cursor-wait"
        style={{ ...CTA_BUTTON_STYLE, boxShadow: CTA_BUTTON_SHADOW }}
      >
        {t('action.send')}
      </Button>
    </form>
  )
}
