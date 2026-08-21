import { useTranslation } from 'react-i18next'
import { Shield } from 'lucide-react'

/** Bandeau d'information : la création de comptes est réservée aux administrateurs. */
export function ProtectionRulesPanel() {
  const { t } = useTranslation('adminUsers')

  return (
    <div className="mb-[22px] flex items-center gap-2.5 rounded-xl bg-accent-dim px-4 py-3">
      <Shield className="size-[18px] shrink-0 text-status-green" aria-hidden="true" />
      <p className="text-[13.5px] leading-snug text-fg-1">{t('banner')}</p>
    </div>
  )
}
