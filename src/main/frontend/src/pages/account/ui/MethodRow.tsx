import { Mail, Pause, Smartphone } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Button, CTA_BUTTON_STYLE } from '@/shared/ui'
import type { MethodState } from '@/features/two-factor'

type Status = 'enabled' | 'disabled' | 'paused' | 'unavailable'

const BADGE: Record<Status, string> = {
  enabled: 'bg-status-green-dim text-status-green',
  disabled: 'bg-status-red-dim text-status-red',
  paused: 'bg-status-orange-dim text-status-orange',
  unavailable: 'bg-bg-3 text-fg-2',
}

export function StatusBadge({ status }: { status: Status }) {
  const { t } = useTranslation('account')
  return (
    <span className={`shrink-0 inline-flex items-center px-2 py-[3px] rounded-[6px] text-[11px] font-bold uppercase whitespace-nowrap ${BADGE[status]}`}>
      {t(`twofa.status_${status}`)}
    </span>
  )
}

interface MethodRowProps {
  state: MethodState
  email: string
  busy: boolean
  onEnable: () => void
  onDisable: () => void
}

/** One method: on, off, paused (on while mail is off) or unavailable (off while mail is off). */
export function MethodRow({ state, email, busy, onEnable, onDisable }: MethodRowProps) {
  const { t } = useTranslation('account')
  const app = state.method === 'APP'
  const paused = state.enabled && !state.usable
  const unavailable = !state.enabled && !state.usable
  const status: Status = paused ? 'paused' : state.enabled ? 'enabled' : unavailable ? 'unavailable' : 'disabled'
  const Icon = paused ? Pause : app ? Smartphone : Mail
  const description = app
    ? t('twofa.app_desc')
    : paused ? t('twofa.mail_paused') : unavailable ? t('twofa.mail_unavailable') : t('twofa.mail_desc', { address: email })

  return (
    <div className="grid grid-cols-[40px_minmax(0,1fr)] gap-x-3.5 gap-y-2 border-t border-border px-7 py-4 sm:grid-cols-[40px_minmax(0,1fr)_auto] sm:items-center">
      <span className={`grid size-10 place-items-center rounded-[11px] ${status === 'enabled' ? 'bg-accent-dim text-accent' : 'bg-bg-3 text-fg-3'}`}>
        <Icon className="size-[18px]" aria-hidden="true" />
      </span>
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2 text-sm font-semibold text-fg-0">
          {t(app ? 'twofa.app_title' : 'twofa.mail_title')}
          <StatusBadge status={status} />
        </div>
        <p className="mt-0.5 text-[12.5px] leading-normal text-fg-2">{description}</p>
      </div>
      {state.enabled ? (
        <Button
          onClick={onDisable}
          disabled={busy}
          className="col-start-2 justify-self-start sm:col-start-auto border-status-red/30 bg-bg-1 text-status-red hover:bg-status-red-dim hover:text-status-red"
        >
          {t('twofa.btn_disable')}
        </Button>
      ) : !unavailable && (
        <Button
          onClick={onEnable}
          disabled={busy}
          className="col-start-2 justify-self-start sm:col-start-auto border-transparent font-semibold"
          style={CTA_BUTTON_STYLE}
        >
          {t('twofa.btn_enable')}
        </Button>
      )}
    </div>
  )
}
