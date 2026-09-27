import { useTranslation } from 'react-i18next'
import type { LucideIcon } from 'lucide-react'
import { DashboardCard } from './DashboardCard'

/** A block whose source failed, in the block's own place — the other blocks stay useful. */
export function UnavailableCard({ icon, title }: { icon: LucideIcon; title: string }) {
  const { t } = useTranslation('dashboard')
  return (
    <DashboardCard icon={icon} title={title}>
      <p className="py-2 text-sm text-fg-3">{t('card.unavailable')}</p>
    </DashboardCard>
  )
}
