import { useTranslation } from 'react-i18next'
import type { SettingSource } from '../model/types'

/** Where a value comes from. A value saved from this page carries no badge: it is the ordinary case. */
export function SourceBadge({ source, variable }: { source: SettingSource; variable: string }) {
  const { t } = useTranslation('adminSettings')
  if (source === 'DATABASE') return null
  const environment = source === 'ENVIRONMENT'
  return (
    <span
      title={environment ? t('source.environment_hint', { variable }) : undefined}
      className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${environment ? 'bg-status-orange-dim text-status-orange' : 'bg-bg-2 text-fg-3'}`}
    >
      {t(`source.${source}`)}
    </span>
  )
}
