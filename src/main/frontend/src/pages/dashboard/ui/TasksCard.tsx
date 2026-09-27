import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { CalendarX2, ChevronRight, SquareCheck } from 'lucide-react'
import { ROUTES } from '@/shared/config'
import type { TaskItem, TasksCard as TasksCardData } from '@/entities/dashboard'
import { useDashboardActions } from '../model/dashboardActions'
import { filterMine } from '../lib/filterMine'
import { DashboardCard } from './DashboardCard'
import { CardGroup } from './CardGroup'
import { TaskRow } from './TaskRow'

/** Rows per group on the dashboard; the rest is one tap away on the board. */
const GROUP_LIMIT = 5

type Mode = 'mine' | 'all'

/**
 * Open tasks in three groups that never overlap, and never repeat today's to-dos (those live in
 * "Aujourd'hui"). In a shared space it opens on the caller's tasks — assigned to them or to nobody —
 * because somebody else's chore does not call for their action; "Tous" shows the household's.
 */
export function TasksCard({ card, dueToday = [] }: { card: TasksCardData; dueToday?: TaskItem[] }) {
  const { t } = useTranslation('dashboard')
  const { spaceId, isShared, currentUserId } = useDashboardActions()
  const [mode, setMode] = useState<Mode>(isShared ? 'mine' : 'all')

  const pick = (tasks: TaskItem[]) => (mode === 'mine' ? filterMine(tasks, currentUserId) : tasks).slice(0, GROUP_LIMIT)
  const overdue = pick(card.overdue)
  const thisWeek = pick(card.thisWeek)
  const inProgress = pick(card.inProgress)
  const shown = overdue.length + thisWeek.length + inProgress.length
  // Today's to-dos are open tasks too, already on screen under "Aujourd'hui": not "other" ones.
  const shownToday = (mode === 'mine' ? filterMine(dueToday, currentUserId) : dueToday).length
  const more = Math.max(0, (mode === 'mine' ? card.openCountMine : card.openCount) - shown - shownToday)

  const toggle = isShared ? (
    <div role="group" aria-label={t('tasks.filter_label')} className="flex rounded-[9px] bg-bg-3 p-0.5">
      {(['mine', 'all'] as const).map((value) => (
        <button key={value} type="button" aria-pressed={mode === value} onClick={() => setMode(value)}
          className={`rounded-[7px] px-2.5 py-1 text-[12.5px] font-semibold ${mode === value ? 'bg-bg-1 text-fg-0 shadow-sm' : 'text-fg-3'}`}>
          {t(value === 'mine' ? 'tasks.mine' : 'tasks.all')}
        </button>
      ))}
    </div>
  ) : undefined

  const footer = more > 0 ? (
    <Link to={ROUTES.spaceOrganisationTasks(spaceId)} className="inline-flex items-center gap-0.5 py-1 text-[13px] font-semibold text-fg-2 hover:text-fg-0">
      {t('tasks.more', { count: more })}<ChevronRight className="size-[15px]" aria-hidden="true" />
    </Link>
  ) : undefined

  return (
    <DashboardCard icon={SquareCheck} title={t('tasks.title')} controls={toggle}
      link={{ to: ROUTES.spaceOrganisationTasks(spaceId), label: t('tasks.link') }} footer={footer}>
      {overdue.length > 0 && (
        <CardGroup title={t('tasks.overdue')} tone="danger" icon={CalendarX2}>
          {overdue.map((task) => <TaskRow key={task.id} task={task} dateStyle="overdue" />)}
        </CardGroup>
      )}
      {thisWeek.length > 0 && (
        <CardGroup title={t('tasks.this_week')}>
          {thisWeek.map((task) => <TaskRow key={task.id} task={task} dateStyle="weekday" />)}
        </CardGroup>
      )}
      {inProgress.length > 0 && (
        <CardGroup title={t('tasks.in_progress')}>
          {inProgress.map((task) => <TaskRow key={task.id} task={task} dateStyle="loose" />)}
        </CardGroup>
      )}
      {shown === 0 && <p className="py-3 text-sm text-fg-3">{t('tasks.none_mine')}</p>}
    </DashboardCard>
  )
}
