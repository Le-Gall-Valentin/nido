import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import i18next from 'i18next'
import { PiggyBank } from 'lucide-react'
import { ROUTES } from '@/shared/config'
import { formatAmount, resolveLocale } from '@/shared/lib'
import type { SavingsCard as SavingsCardData, SavingsGoalItem } from '@/entities/dashboard'
import { safeSavingsGoalColor, safeSavingsGoalGlyph } from '@/widgets/savings-goal'
import { useDashboardActions } from '../model/dashboardActions'
import { formatDayMonth, formatMonthYear } from '../lib/dates'
import { DashboardCard } from './DashboardCard'
import { CardList, CardRow } from './CardRow'

function GoalRow({ goal }: { goal: SavingsGoalItem }) {
  const { t } = useTranslation('dashboard')
  const locale = resolveLocale(i18next.language)
  const reached = goal.state === 'REACHED'
  const percent = goal.target > 0 ? Math.min(100, (goal.contributed / goal.target) * 100) : 100
  const color = safeSavingsGoalColor(goal.color)
  const date = (iso: string | null, long: boolean) => (iso ? (long ? formatMonthYear(iso, locale) : formatDayMonth(iso, locale)) : '')

  let meta: ReactNode
  switch (goal.state) {
    case 'REACHED':
      meta = <span className="text-status-green">{t('savings.reached')}</span>
      break
    case 'PAST_DUE':
      meta = <span className="text-status-orange">{t('savings.past_due', { date: date(goal.targetDate, false) })}</span>
      break
    case 'DUE_SOON':
      meta = <span className="text-status-orange">{t('savings.due_soon', { date: date(goal.targetDate, false) })}</span>
      break
    case 'IN_PROGRESS':
      meta = goal.monthlyNeeded !== null && goal.targetDate
        ? <span>{t('savings.monthly_needed', {
            remaining: formatAmount(goal.target - goal.contributed), monthly: formatAmount(goal.monthlyNeeded), date: date(goal.targetDate, true),
          })}</span>
        : <span>{t('savings.no_target_date')}</span>
      break
  }

  return (
    <CardRow
      lead={<span className="grid size-8 shrink-0 place-items-center rounded-[10px] text-base" style={{ background: color }}>{safeSavingsGoalGlyph(goal.glyph)}</span>}
      title={<span className="flex items-baseline justify-between gap-3">
        <span className="truncate">{goal.name}</span>
        <span className="shrink-0 font-semibold tabular-nums text-fg-1">
          {reached ? formatAmount(goal.target) : `${formatAmount(goal.contributed)} / ${formatAmount(goal.target)}`}
        </span>
      </span>}
      extra={<>
        <div className="mt-1.5 h-1.5 overflow-hidden rounded-full bg-bg-3">
          <span className={`block h-full rounded-full ${reached ? 'bg-status-green' : ''}`} style={{ width: `${percent}%`, background: reached ? undefined : color }} />
        </div>
        <div className="mt-1 text-[12.5px] text-fg-3">{meta}</div>
      </>} />
  )
}

/** Shared goals: how far each one is, and what it takes to get there in time. */
export function SavingsCard({ card }: { card: SavingsCardData }) {
  const { t } = useTranslation('dashboard')
  const { spaceId } = useDashboardActions()
  return (
    <DashboardCard icon={PiggyBank} title={t('savings.title')} link={{ to: ROUTES.spaceFinance(spaceId), label: t('savings.link') }}>
      <CardList>{card.goals.map((goal) => <GoalRow key={goal.goalId} goal={goal} />)}</CardList>
    </DashboardCard>
  )
}
