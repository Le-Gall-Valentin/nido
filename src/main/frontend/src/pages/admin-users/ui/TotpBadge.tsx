import { useTranslation } from 'react-i18next'

interface TotpBadgeProps {
  enabled: boolean
}

export function TotpBadge({ enabled }: TotpBadgeProps) {
  const { t } = useTranslation('adminUsers')

  if (enabled) {
    return (
      <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full border border-status-green/20 bg-status-green-dim text-[11px] font-medium text-status-green">
        <span className="size-1.5 rounded-full bg-status-green" aria-hidden="true" />
        {t('table.totp_on')}
      </span>
    )
  }

  return (
    <span className="inline-flex items-center px-2 py-0.5 rounded-full border border-border bg-bg-2 text-[11px] font-medium text-fg-2">
      {t('table.totp_off')}
    </span>
  )
}
