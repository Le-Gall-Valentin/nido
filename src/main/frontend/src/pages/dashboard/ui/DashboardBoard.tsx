import { Fragment, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Calendar, PiggyBank, ShoppingCart, SquareCheck, Utensils, Wallet, type LucideIcon } from 'lucide-react'
import type { CardResult, Dashboard } from '@/entities/dashboard'
import type { ZonedNow } from '../lib/useNow'
import { UnavailableCard } from './UnavailableCard'
import { AttentionCard } from './AttentionCard'
import { TodayCard } from './TodayCard'
import { MenuCard } from './MenuCard'
import { FinanceCard } from './FinanceCard'
import { TasksCard } from './TasksCard'
import { ShoppingCard } from './ShoppingCard'
import { SavingsCard } from './SavingsCard'

const SEVEN_FIVE = 'grid grid-cols-1 gap-4 lg:grid-cols-[minmax(0,7fr)_minmax(0,5fr)]'

/** Absent → nothing; unavailable → the unavailable card in the block's place; otherwise the block. */
function present<T>(result: CardResult<T> | null | undefined, render: (data: T) => ReactNode,
  fallback: { icon: LucideIcon; title: string }): ReactNode {
  if (!result) return null
  if (result.status === 'UNAVAILABLE') return <UnavailableCard icon={fallback.icon} title={fallback.title} />
  return render(result.data)
}

function TodayRow({ left, right }: { left: ReactNode; right: ReactNode }) {
  if (left && right) return <div data-layout="pair" className={SEVEN_FIVE}>{left}{right}</div>
  return <>{left ?? right}</>
}

/** Row four: tasks beside a stack of shopping and savings — whose last card stretches so both columns end level. */
function TasksRow({ tasks, side }: { tasks: ReactNode; side: { key: string; node: ReactNode }[] }) {
  const sideNodes = side.map(({ key, node }) => <Fragment key={key}>{node}</Fragment>)
  if (!tasks && side.length === 0) return null
  if (!tasks) {
    return <div data-layout="side-only" className={`grid grid-cols-1 gap-4 ${side.length > 1 ? 'lg:grid-cols-2' : ''}`}>{sideNodes}</div>
  }
  if (side.length === 0) return <div data-layout="tasks-only">{tasks}</div>
  return (
    <div data-layout="tasks-with-side" className={SEVEN_FIVE}>
      {tasks}
      <div className="flex min-w-0 flex-col gap-4 [&>*:last-child]:flex-1">{sideNodes}</div>
    </div>
  )
}

/**
 * The page's grid, the mockup's: "À traiter" (only when something needs an action), today beside the
 * menu, finances across the full width, then tasks beside shopping and savings. 7/5 columns from
 * 1024 px, one column below it in that same order. The server decides which blocks exist; this only
 * decides where they go, and closes the gap a missing one would leave.
 */
export function DashboardBoard({ dashboard, now }: { dashboard: Dashboard; now: ZonedNow }) {
  const { t } = useTranslation('dashboard')
  const { attention, cards, date } = dashboard

  const agenda = present(cards.agenda, (data) => <TodayCard card={data} date={date} now={now} />, { icon: Calendar, title: t('today.title') })
  const menu = present(cards.menu, (data) => <MenuCard card={data} date={date} />, { icon: Utensils, title: t('menu.title') })
  const finance = present(cards.finance, (data) => <FinanceCard card={data} />, { icon: Wallet, title: t('finance.title') })
  const tasks = present(cards.tasks, (data) => <TasksCard card={data} />, { icon: SquareCheck, title: t('tasks.title') })
  const side = [
    { key: 'shopping', node: present(cards.shopping, (data) => <ShoppingCard card={data} />, { icon: ShoppingCart, title: t('shopping.title') }) },
    { key: 'savings', node: present(cards.savings, (data) => <SavingsCard card={data} />, { icon: PiggyBank, title: t('savings.title') }) },
  ].filter((entry) => entry.node !== null)

  return (
    <div className="flex flex-col gap-4">
      {attention.length > 0 && <AttentionCard items={attention} />}
      <TodayRow left={agenda} right={menu} />
      {finance}
      <TasksRow tasks={tasks} side={side} />
    </div>
  )
}
