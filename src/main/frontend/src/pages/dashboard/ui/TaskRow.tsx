import { useTranslation } from 'react-i18next'
import i18next from 'i18next'
import { ListChecks } from 'lucide-react'
import { resolveLocale } from '@/shared/lib'
import { useChangeTaskStatus } from '@/entities/tasks'
import type { TaskItem } from '@/entities/dashboard'
import { TASK_PRIORITY_META } from '@/widgets/task-management'
import { useDashboardActions } from '../model/dashboardActions'
import { formatDayMonth, formatShortDay } from '../lib/dates'
import { CardRow } from './CardRow'
import { AvatarStack } from './AvatarStack'

/** How a row states its due date, depending on the group it sits in. */
export type TaskDateStyle = 'none' | 'overdue' | 'weekday' | 'loose'

/**
 * One task, tickable from the dashboard. A task with open subtasks cannot be done — the server refuses
 * that transition — so the box says so instead of failing: `aria-disabled` rather than `disabled`, so
 * its tooltip still shows on hover.
 */
export function TaskRow({ task, dateStyle }: { task: TaskItem; dateStyle: TaskDateStyle }) {
  const { t } = useTranslation('dashboard')
  const { spaceId, canWrite, currentUserId, reportError } = useDashboardActions()
  const changeStatus = useChangeTaskStatus(spaceId)
  const locale = resolveLocale(i18next.language)
  const openSubtasks = task.subtasksTotal - task.subtasksDone
  const blocked = openSubtasks > 0
  const priority = TASK_PRIORITY_META[task.priority]
  const yourTurn = task.recurring && currentUserId !== null && task.assigneeIds.includes(currentUserId)

  let date = null
  if (dateStyle === 'loose' && !task.dueDate) {
    date = <span>{t('tasks.no_due_date')}</span>
  } else if (task.dueDate && dateStyle === 'overdue') {
    date = <span className="text-status-red">{t('tasks.due_on', { date: formatDayMonth(task.dueDate, locale) })}</span>
  } else if (task.dueDate && dateStyle === 'weekday') {
    date = <span>{formatShortDay(task.dueDate, locale)}</span>
  } else if (task.dueDate && dateStyle === 'loose') {
    date = <span>{formatDayMonth(task.dueDate, locale)}</span>
  }

  const checkbox = canWrite ? (
    <button type="button" role="checkbox" aria-checked={false}
      aria-label={t('tasks.mark_done', { title: task.title })}
      aria-disabled={blocked || changeStatus.isPending}
      title={blocked ? t('tasks.subtasks_left', { count: openSubtasks }) : undefined}
      onClick={() => {
        if (blocked || changeStatus.isPending) return
        changeStatus.mutate({ taskId: task.id, status: 'DONE' }, { onError: reportError })
      }}
      className="size-5 shrink-0 rounded-md border-2 border-border-2 transition-colors hover:border-accent aria-disabled:cursor-not-allowed aria-disabled:opacity-50" />
  ) : null

  return (
    <CardRow lead={checkbox} title={task.title}
      meta={<>
        {date}
        {yourTurn && <span className="font-semibold text-accent">{t('tasks.your_turn')}</span>}
        <span className={`inline-flex items-center gap-1 font-semibold ${priority.textClassName}`}>
          <span className={`size-1.5 rounded-full ${priority.dotClassName}`} />{t(`priority.${task.priority}`)}
        </span>
        {task.subtasksTotal > 0 && (
          <span className="inline-flex items-center gap-1">
            <ListChecks className="size-3.5" aria-hidden="true" />{task.subtasksDone}/{task.subtasksTotal}
          </span>
        )}
      </>}
      trail={task.assigneeIds.length > 0 ? <AvatarStack memberIds={task.assigneeIds} /> : undefined} />
  )
}
