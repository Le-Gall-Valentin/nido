import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import i18next from 'i18next'
import { CalendarPlus, ShoppingBasket, Utensils } from 'lucide-react'
import { ROUTES } from '@/shared/config'
import { resolveLocale } from '@/shared/lib'
import type { MealItem, MenuCard as MenuCardData } from '@/entities/dashboard'
import { useDashboardActions } from '../model/dashboardActions'
import { addDaysIso } from '../lib/dates'
import { formatDayRanges } from '../lib/formatDayRanges'
import { DashboardCard } from './DashboardCard'
import { CardRow } from './CardRow'
import { CardGroup } from './CardGroup'
import { SmallLink } from './cardParts'
import { ExportWeekDialog } from './ExportWeekDialog'

const WEEK_LENGTH = 7

function MealRow({ meal }: { meal: MealItem }) {
  const { t } = useTranslation('dashboard')
  return (
    <CardRow
      title={meal.recipeName ?? <span className="italic text-fg-3">{t('menu.deleted_recipe')}</span>}
      meta={<>
        {meal.category && (
          <span className="rounded-md bg-bg-3 px-1.5 py-px text-[11.5px] font-semibold text-fg-2">{t(`menu.category.${meal.category}`)}</span>
        )}
        <span>{t('menu.portions', { count: meal.portions })}</span>
        {meal.minutes !== null && <span>{t('menu.minutes', { count: meal.minutes })}</span>}
      </>} />
  )
}

/** Today's and tomorrow's meals, the days still to plan this week, and the week's shopping in one click. */
export function MenuCard({ card, date }: { card: MenuCardData; date: string }) {
  const { t } = useTranslation('dashboard')
  const { spaceId, canWrite } = useDashboardActions()
  const [exporting, setExporting] = useState(false)
  const locale = resolveLocale(i18next.language)
  const nothingPlanned = card.unplannedDays.length >= WEEK_LENGTH
  const unplanned = nothingPlanned
    ? t('menu.nothing_this_week')
    : t('menu.unplanned', { days: formatDayRanges(card.unplannedDays, locale, (from, to) => t('menu.range', { from, to })) })

  const footer = canWrite && !nothingPlanned ? (
    <button type="button" onClick={() => setExporting(true)}
      className="inline-flex items-center gap-1.5 py-1 text-[13px] font-semibold text-fg-2 hover:text-fg-0">
      <ShoppingBasket className="size-4" aria-hidden="true" />{t('menu.send_week')}
    </button>
  ) : undefined

  return (
    <DashboardCard icon={Utensils} title={t('menu.title')} link={{ to: ROUTES.spaceKitchenMenu(spaceId), label: t('menu.link') }} footer={footer}>
      {card.today.length > 0 && (
        <CardGroup title={t('menu.today')}>{card.today.map((meal) => <MealRow key={meal.entryId} meal={meal} />)}</CardGroup>
      )}
      {card.tomorrow.length > 0 && (
        <CardGroup title={t('menu.tomorrow')}>{card.tomorrow.map((meal) => <MealRow key={meal.entryId} meal={meal} />)}</CardGroup>
      )}
      {card.unplannedDays.length > 0 && (
        <div className="mt-2 flex items-center gap-2.5 rounded-xl bg-status-orange-dim py-2 pl-3 pr-2 text-[13.5px] font-medium text-status-orange">
          <CalendarPlus className="size-4 shrink-0" aria-hidden="true" />
          <span className="min-w-0 flex-1">{unplanned}</span>
          {canWrite && <SmallLink to={ROUTES.spaceKitchenMenu(spaceId)}>{t('menu.plan')}</SmallLink>}
        </div>
      )}
      {exporting && (
        <ExportWeekDialog spaceId={spaceId} from={date} to={addDaysIso(date, WEEK_LENGTH - 1)} onClose={() => setExporting(false)} />
      )}
    </DashboardCard>
  )
}
