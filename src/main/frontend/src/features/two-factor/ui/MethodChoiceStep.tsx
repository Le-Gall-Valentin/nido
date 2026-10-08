import { useState } from 'react'
import { AlertTriangle, ChevronLeft, Info, Lock } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { NetworkError } from '@/shared/lib'
import { MethodCard } from './MethodCard'
import type { ITwoFactorChallengeApi } from '../model/ITwoFactorChallengeApi'
import type { CodeChoice } from '../model/types'
import { ChallengeExpiredError, MethodNotEnabledError, MethodUnavailableError, ResendTooSoonError, SendLimitError } from '../model/errors'

interface MethodChoiceStepProps {
  username: string
  maskedEmail: string | null
  api: Pick<ITwoFactorChallengeApi, 'sendMailCode'>
  onChoose: (choice: CodeChoice) => void
  onBack: () => void
}

/** Both methods on: the person picks. The mail's code leaves on the click — never from an effect. */
export function MethodChoiceStep({ username, maskedEmail, api, onChoose, onBack }: MethodChoiceStepProps) {
  const { t } = useTranslation('twoFactor')
  const [sending, setSending] = useState(false)
  const [error, setError] = useState<{ key: string; values?: Record<string, unknown> } | null>(null)

  async function chooseMail() {
    if (sending) return
    setSending(true)
    setError(null)
    try {
      const { resendAfterSeconds } = await api.sendMailCode()
      onChoose({ method: 'MAIL', resendAfterSeconds })
    } catch (e) {
      // A code sent less than a minute ago still works: go to it, with the time left.
      if (e instanceof ResendTooSoonError) onChoose({ method: 'MAIL', resendAfterSeconds: e.seconds })
      else if (e instanceof SendLimitError) setError({ key: 'choose.error.send_limit', values: { minutes: Math.ceil(e.seconds / 60) } })
      else if (e instanceof MethodUnavailableError) setError({ key: 'choose.error.mail_unavailable' })
      else if (e instanceof MethodNotEnabledError) setError({ key: 'choose.error.mail_not_enabled' })
      else if (e instanceof ChallengeExpiredError) setError({ key: 'choose.error.challenge_expired' })
      else if (e instanceof NetworkError) setError({ key: 'choose.error.network' })
      else setError({ key: 'choose.error.server' })
    } finally {
      setSending(false)
    }
  }

  return (
    <div>
      <button
        type="button"
        onClick={onBack}
        disabled={sending}
        className="mb-5 flex items-center gap-1 bg-transparent border-0 p-0 text-[13.5px] text-fg-2 cursor-pointer hover:text-fg-0"
      >
        <ChevronLeft className="size-[15px]" />
        {t('verify.back')}
      </button>

      <div className="mb-[18px] grid size-[46px] place-items-center rounded-[13px] bg-accent-dim text-accent">
        <Lock className="size-6" />
      </div>

      <div className="mb-6">
        <h2 className="mb-1.5 text-[24px] lg:text-[28px] font-semibold tracking-tight text-fg-0">{t('choose.title')}</h2>
        <p className="text-[15px] text-fg-2">{t('choose.subtitle', { username })}</p>
      </div>

      <div className="grid gap-2.5">
        <MethodCard method="APP" title={t('method.app_title')} description={t('method.app_choice')}
          onSelect={() => onChoose({ method: 'APP' })} disabled={sending} />
        <MethodCard method="MAIL" title={t('method.mail_title')} description={t('method.mail_choice', { address: maskedEmail })}
          onSelect={() => void chooseMail()} disabled={sending} />
      </div>

      {error && (
        <div role="alert" className="mt-3 flex items-center gap-2 rounded-[10px] bg-status-red-dim px-3.5 py-[11px] text-[13.5px] text-status-red">
          <AlertTriangle className="size-3.5 shrink-0" />
          {t(error.key, error.values)}
        </div>
      )}

      <div className="mt-7 flex gap-2 items-start text-[13px] text-fg-2">
        <Info className="size-3.5 shrink-0 mt-0.5" />
        <span>{t('choose.help')}</span>
      </div>
    </div>
  )
}
