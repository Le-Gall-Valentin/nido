import { useTranslation } from 'react-i18next'
import { ShoppingCart } from 'lucide-react'
import { ROUTES } from '@/shared/config'
import type { ShoppingCard as ShoppingCardData } from '@/entities/dashboard'
import { useDashboardActions } from '../model/dashboardActions'
import { DashboardCard } from './DashboardCard'
import { CardList, CardRow } from './CardRow'
import { Counter } from './cardParts'

/** What is left to buy, aisle by aisle in the list's own order. */
export function ShoppingCard({ card }: { card: ShoppingCardData }) {
  const { t } = useTranslation('dashboard')
  const { spaceId } = useDashboardActions()
  return (
    <DashboardCard icon={ShoppingCart} title={t('shopping.title')} aside={<Counter>{t('shopping.count', { count: card.remaining })}</Counter>}
      link={{ to: ROUTES.spaceOrganisationCourses(spaceId), label: t('shopping.link') }}>
      <CardList>
        {card.categories.map((group) => {
          const hidden = group.count - group.preview.length
          const preview = group.preview.join(', ') + (hidden > 0 ? ` ${t('shopping.and_more', { count: hidden })}` : '')
          return (
            <CardRow key={group.categoryId} title={group.name} meta={<span className="truncate">{preview}</span>}
              trail={<span className="text-sm font-semibold tabular-nums text-fg-1">{group.count}</span>} />
          )
        })}
      </CardList>
    </DashboardCard>
  )
}
