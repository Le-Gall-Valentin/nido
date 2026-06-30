import { ShieldCheck } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Button, CTA_BUTTON_STYLE } from '@/shared/ui'

interface TotpEnrollProposalProps {
  username: string
  onActivate: () => void
  onSkip: () => void
}

export function TotpEnrollProposal({ username, onActivate, onSkip }: TotpEnrollProposalProps) {
  const { t } = useTranslation('totp')

  return (
    <div>
      <div className="mb-8">
        <h2 className="mb-2 text-[24px] lg:text-[28px] font-semibold tracking-tight text-fg-0">
          {t('enroll.title')}
        </h2>
        <p className="text-sm text-fg-2">
          {t('enroll.subtitle', { username })}
        </p>
      </div>

      <div className="rounded-lg border border-border bg-bg-2 p-3.5 my-1 mb-3.5">
        <div className="flex gap-3 items-start">
          <div className="size-9 rounded-lg bg-accent-dim text-accent grid place-items-center shrink-0">
            <ShieldCheck className="size-[18px]" />
          </div>
          <div>
            <div className="text-[13px] font-semibold mb-1 text-fg-0">
              {t('enroll.why_title')}
            </div>
            <div className="text-xs text-fg-2 leading-relaxed">
              {t('enroll.why_text')}
            </div>
          </div>
        </div>
      </div>

      <Button
        type="button"
        onClick={onActivate}
        className="mt-2 w-full border-transparent py-3 font-semibold active:translate-y-px"
        style={CTA_BUTTON_STYLE}
      >
        <ShieldCheck className="size-4" />
        {t('enroll.activate')}
      </Button>

      <button
        type="button"
        onClick={onSkip}
        className="w-full mt-2 bg-transparent border-0 text-fg-2 text-xs cursor-pointer py-1.5 hover:text-fg-0 text-center"
      >
        {t('enroll.skip')}
      </button>
    </div>
  )
}