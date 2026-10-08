import { Mail, Pause, Smartphone } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import type { TwoFactorMethod } from '@/entities/user'
import type { MailAvailability } from '@/entities/capabilities'

interface TwoFactorChipsProps {
  methods: TwoFactorMethod[]
  /** Whether mail works: the mail method on while it does not is paused. */
  mail: MailAvailability
}

const CHIP = 'inline-flex items-center gap-[5px] px-2 py-[3px] rounded-[6px] text-[11px] font-semibold whitespace-nowrap'

/** Which methods protect an account — the first thing to look at when someone calls about a lost phone. */
export function TwoFactorChips({ methods, mail }: TwoFactorChipsProps) {
  const { t } = useTranslation('adminUsers')
  if (methods.length === 0) {
    return <span className={`${CHIP} bg-bg-3 text-fg-2`}>{t('table.two_factor_none')}</span>
  }
  return (
    <span className="flex flex-wrap gap-1.5">
      {methods.includes('APP') && (
        <span className={`${CHIP} bg-status-green-dim text-status-green`}>
          <Smartphone className="size-3" aria-hidden="true" />{t('table.two_factor_app')}
        </span>
      )}
      {methods.includes('MAIL') && (mail === 'unavailable' ? (
        <span className={`${CHIP} bg-status-orange-dim text-status-orange`}>
          <Pause className="size-3" aria-hidden="true" />{t('table.two_factor_mail_paused')}
        </span>
      ) : (
        <span className={`${CHIP} bg-status-green-dim text-status-green`}>
          <Mail className="size-3" aria-hidden="true" />{t('table.two_factor_mail')}
        </span>
      ))}
    </span>
  )
}
