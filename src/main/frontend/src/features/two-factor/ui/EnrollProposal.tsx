import { useState } from 'react'
import { AlertTriangle, ShieldCheck } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Button, CTA_BUTTON_STYLE } from '@/shared/ui'
import { MethodCard } from './MethodCard'
import type { ITwoFactorMethodsApi } from '../model/ITwoFactorMethodsApi'
import type { MailSetupData } from '../model/types'
import { commonErrorMessage, type Message } from '../model/messages'
import { MethodUnavailableError, ResendTooSoonError } from '../model/errors'

interface EnrollProposalProps {
  username: string
  /** The account's address — its own, shown whole: the password is already right. */
  email: string
  /** Mail off: only the app is offered, as the screen always was. */
  mailAvailable: boolean
  api: Pick<ITwoFactorMethodsApi, 'setupMail'>
  onAppChosen: () => void
  onMailStarted: (setup: MailSetupData) => void
  onSkip: () => void
}

export function EnrollProposal({ username, email, mailAvailable, api, onAppChosen, onMailStarted, onSkip }: EnrollProposalProps) {
  const { t } = useTranslation('twoFactor')
  const [sending, setSending] = useState(false)
  const [error, setError] = useState<Message | null>(null)

  async function chooseMail() {
    if (sending) return
    setSending(true)
    setError(null)
    try {
      onMailStarted(await api.setupMail())
    } catch (e) {
      if (e instanceof ResendTooSoonError) onMailStarted({ sentTo: email, resendAfterSeconds: e.seconds })
      else if (e instanceof MethodUnavailableError) setError({ key: 'enroll.error.mail_unavailable' })
      else setError(commonErrorMessage(e))
    } finally {
      setSending(false)
    }
  }

  const skip = (
    <button
      type="button"
      onClick={onSkip}
      className="w-full mt-2 bg-transparent border-0 text-fg-2 text-xs cursor-pointer py-1.5 hover:text-fg-0 text-center"
    >
      {t('enroll.skip')}
    </button>
  )

  if (!mailAvailable) {
    return (
      <div>
        <div className="mb-8">
          <h2 className="mb-2 text-[24px] lg:text-[28px] font-semibold tracking-tight text-fg-0">{t('enroll.title')}</h2>
          <p className="text-sm text-fg-2">{t('enroll.subtitle', { username })}</p>
        </div>
        <div className="rounded-lg border border-border bg-bg-2 p-3.5 my-1 mb-3.5">
          <div className="flex gap-3 items-start">
            <div className="size-9 rounded-lg bg-accent-dim text-accent grid place-items-center shrink-0">
              <ShieldCheck className="size-[18px]" />
            </div>
            <div>
              <div className="text-[13px] font-semibold mb-1 text-fg-0">{t('enroll.why_title')}</div>
              <div className="text-xs text-fg-2 leading-relaxed">{t('enroll.why_text')}</div>
            </div>
          </div>
        </div>
        <Button
          type="button"
          onClick={onAppChosen}
          className="mt-2 w-full border-transparent py-3 font-semibold active:translate-y-px"
          style={CTA_BUTTON_STYLE}
        >
          <ShieldCheck className="size-4" />
          {t('enroll.activate')}
        </Button>
        {skip}
      </div>
    )
  }

  return (
    <div>
      <div className="mb-6">
        <h2 className="mb-2 text-[24px] lg:text-[28px] font-semibold tracking-tight text-fg-0">{t('enroll.title')}</h2>
        <p className="text-sm text-fg-2">{t('enroll.subtitle_choice', { username })}</p>
      </div>
      <p className="mb-2 text-[12.5px] font-semibold text-fg-1">{t('enroll.question')}</p>
      <div className="grid gap-2.5">
        <MethodCard method="APP" title={t('method.app_title')} description={t('method.app_enroll')} tag={t('method.app_tag')}
          onSelect={onAppChosen} disabled={sending} />
        <MethodCard method="MAIL" title={t('method.mail_title')} description={t('method.mail_enroll', { address: email })}
          onSelect={() => void chooseMail()} disabled={sending} />
      </div>
      {error && (
        <div role="alert" className="mt-3 flex items-center gap-2 rounded-[10px] bg-status-red-dim px-3.5 py-[11px] text-[13.5px] text-status-red">
          <AlertTriangle className="size-3.5 shrink-0" />
          {t(error.key, error.values)}
        </div>
      )}
      {skip}
    </div>
  )
}
