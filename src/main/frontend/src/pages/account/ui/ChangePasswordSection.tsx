import { useState, useRef, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Input, CTA_BUTTON_STYLE } from '@/shared/ui'
import { InvalidCurrentPasswordError } from '../api/accountApi'
import { NetworkError, RateLimitError, isValidPassword, passwordProblem } from '@/shared/lib'
import { useFlash } from '../model/useFlash'


/**
 * How long the confirmation stays on screen before onChanged hands over. Long enough to
 * read a sentence, short enough that nobody starts typing again first.
 */
const HANDOVER_DELAY_MS = 2500

interface ChangePasswordSectionProps {
  onChangePassword: (currentPassword: string, newPassword: string) => Promise<void>
  /**
   * Called shortly after a successful change. The server revokes every refresh token on a
   * password change, this device's included, so the caller signs out here rather than
   * leaving the session to die on its next refresh as a generic "session expired".
   */
  onChanged?: () => void
}

export function ChangePasswordSection({ onChangePassword, onChanged }: ChangePasswordSectionProps) {
  const { t } = useTranslation('account')
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [confirm, setConfirm] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const { flash, showFlash } = useFlash(3000)
  const handoverTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  const mismatch = next.length > 0 && confirm.length > 0 && next !== confirm
  const problem = next.length > 0 ? passwordProblem(next) : null
  const canSubmit = current.length > 0 && isValidPassword(next) && next === confirm

  useEffect(() => {
    return () => {
      if (handoverTimer.current) clearTimeout(handoverTimer.current)
    }
  }, [])

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!canSubmit || isSubmitting) return
    setIsSubmitting(true)
    try {
      await onChangePassword(current, next)
      setCurrent('')
      setNext('')
      setConfirm('')
      showFlash('success', 'password.success')
      if (onChanged) {
        handoverTimer.current = setTimeout(onChanged, HANDOVER_DELAY_MS)
      }
    } catch (error) {
      if (error instanceof InvalidCurrentPasswordError) {
        showFlash('error', 'password.error.wrong_current')
      } else if (error instanceof RateLimitError) {
        showFlash('error', 'password.error.rate_limit')
      } else if (error instanceof NetworkError) {
        showFlash('error', 'password.error.network')
      } else {
        showFlash('error', 'password.error.server')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section id="section-password" className="rounded-2xl border border-border bg-bg-1 mb-4 overflow-hidden">
      <div className="px-7 pt-6">
        <h3 className="text-lg font-semibold text-fg-0">{t('password.title')}</h3>
        <p className="text-[13.5px] text-fg-2 mt-0.5">{t('password.subtitle')}</p>
      </div>
      <div className="px-7 py-5">
        <form onSubmit={(e) => void handleSubmit(e)}>
          <div className="mb-3 max-w-[360px]">
            <Input
              label={t('password.current')}
              name="currentPassword"
              type="password"
              value={current}
              onChange={e => setCurrent(e.target.value)}
              autoComplete="current-password"
              disabled={isSubmitting}
            />
          </div>
          <div className="flex flex-col gap-3 sm:flex-row mb-1">
            <div className="min-w-0 sm:flex-1">
              <Input
                label={t('password.new')}
                name="newPassword"
                type="password"
                value={next}
                onChange={e => setNext(e.target.value)}
                autoComplete="new-password"
                disabled={isSubmitting}
              />
            </div>
            <div className="min-w-0 sm:flex-1">
              <Input
                label={t('password.confirm')}
                name="confirmPassword"
                type="password"
                value={confirm}
                onChange={e => setConfirm(e.target.value)}
                autoComplete="new-password"
                disabled={isSubmitting}
              />
            </div>
          </div>
          {mismatch && <p className="text-xs text-status-red mb-2">{t('password.error.mismatch')}</p>}
          {!mismatch && problem && <p className="text-xs text-status-orange mb-2">{t(`password.error.${problem}`)}</p>}
          {flash && (
            <div
              role={flash.kind === 'success' ? 'status' : 'alert'}
              className={`mb-3 text-xs px-3 py-2 rounded-lg ${flash.kind === 'success' ? 'bg-status-green-dim text-status-green' : 'bg-status-red-dim text-status-red'}`}
            >
              {t(flash.key)}
            </div>
          )}
          <div className="flex justify-end mt-2">
            <Button type="submit" disabled={!canSubmit} isLoading={isSubmitting} className="border-transparent font-semibold" style={CTA_BUTTON_STYLE}>
              {t('password.submit')}
            </Button>
          </div>
        </form>
      </div>
    </section>
  )
}