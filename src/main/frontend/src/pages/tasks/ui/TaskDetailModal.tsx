import { useTranslation } from 'react-i18next'
import { Repeat } from 'lucide-react'
import { Dialog, Button } from '@/shared/ui'
import { UserAvatar } from '@/entities/user'
import type { SpaceMember } from '@/entities/space'
import type { RecurringTaskSeries, Task } from '@/entities/tasks'
import { TASK_PRIORITY_META } from '../lib/taskPriorityMeta'

interface TaskDetailModalProps {
  task: Task
  /** The series this task was materialized from, or null for a one-off task (or a
   * recurring occurrence whose series has since been deleted). */
  series: RecurringTaskSeries | null
  members: SpaceMember[]
  onClose: () => void
}

export function TaskDetailModal({ task, series, members, onClose }: TaskDetailModalProps) {
  const { t } = useTranslation('tasks')
  const priorityMeta = TASK_PRIORITY_META[task.priority]

  function memberLabel(memberId: string): string {
    const member = members.find((m) => m.userId === memberId)
    return member?.username ?? member?.email ?? memberId
  }

  return (
    <Dialog open onClose={onClose} title={t('detail.title')} maxWidth="max-w-lg">
      <div className="mb-4">
        <p className="flex items-center gap-1.5 text-[17px] font-semibold text-fg-0">
          {task.title}
          {task.recurring && (
            <span className="flex items-center gap-0.5 rounded-full bg-bg-2 px-1.5 py-0.5 text-[10.5px] font-semibold text-fg-3">
              <Repeat size={10} /> {t('form.recurring_label')}
            </span>
          )}
        </p>
        <p className={`flex items-center gap-1 text-sm font-semibold ${priorityMeta.textClassName}`}>
          <span className={`size-1.5 rounded-full ${priorityMeta.dotClassName}`} />
          {t(priorityMeta.labelKey)}
        </p>
      </div>

      <dl className="space-y-2.5 text-sm">
        <div className="flex items-center justify-between">
          <dt className="text-fg-3">{t('detail.status_label')}</dt>
          <dd className="font-medium text-fg-0">{t(`column.${task.status}`)}</dd>
        </div>
        <div className="flex items-center justify-between">
          <dt className="text-fg-3">{t('detail.due_date_label')}</dt>
          <dd className="font-medium text-fg-0">{task.dueDate ?? t('detail.no_due_date')}</dd>
        </div>
        <div className="flex items-center justify-between">
          <dt className="text-fg-3">{t('detail.created_by_label')}</dt>
          <dd className="flex items-center gap-1.5 font-medium text-fg-0">
            {task.createdBy ? (
              <>
                <UserAvatar username={memberLabel(task.createdBy)} role="USER" className="size-5 rounded-full text-[9px]" />
                {memberLabel(task.createdBy)}
              </>
            ) : t('detail.unknown_creator')}
          </dd>
        </div>
      </dl>

      <div className="mt-4 border-t border-border pt-4">
        <h3 className="mb-2 text-[13px] font-semibold text-fg-1">{t('detail.assignees_label')}</h3>
        {task.assigneeIds.length > 0 ? (
          <ul className="space-y-1.5">
            {task.assigneeIds.map((memberId) => (
              <li key={memberId} className="flex items-center gap-2 text-sm">
                <UserAvatar username={memberLabel(memberId)} role="USER" className="size-6 shrink-0 rounded-full text-[10px]" />
                <span className="flex-1 truncate text-fg-1">{memberLabel(memberId)}</span>
              </li>
            ))}
          </ul>
        ) : (
          <p className="text-sm text-fg-3">{t('detail.no_assignee')}</p>
        )}
      </div>

      {task.subtasks.length > 0 && (
        <div className="mt-4 border-t border-border pt-4">
          <h3 className="mb-2 text-[13px] font-semibold text-fg-1">{t('form.subtasks_title')}</h3>
          <ul className="space-y-1.5">
            {task.subtasks.map((subtask) => (
              <li key={subtask.id} className="flex items-center gap-2 text-sm">
                <span className={`grid size-3.5 shrink-0 place-items-center rounded-[4px] border ${subtask.done ? 'border-status-green bg-status-green' : 'border-border'}`}>
                  {subtask.done && <span className="text-[8px] font-bold text-white">✓</span>}
                </span>
                <span className={subtask.done ? 'text-fg-4 line-through' : 'text-fg-1'}>{subtask.text}</span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {series && series.rotationMemberIds.length > 0 && (
        <div className="mt-4 border-t border-border pt-4">
          <h3 className="mb-2 text-[13px] font-semibold text-fg-1">{t('detail.rotation_participants_label')}</h3>
          <ul className="space-y-1.5">
            {series.rotationMemberIds.map((memberId) => (
              <li key={memberId} className="flex items-center gap-2 text-sm">
                <UserAvatar username={memberLabel(memberId)} role="USER" className="size-6 shrink-0 rounded-full text-[10px]" />
                <span className="flex-1 truncate text-fg-1">{memberLabel(memberId)}</span>
                {task.assigneeIds.includes(memberId) && (
                  <span className="rounded-full bg-accent-dim px-2 py-0.5 text-[10.5px] font-semibold text-accent">
                    {t('detail.this_occurrence_label')}
                  </span>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}

      <div className="mt-5 flex justify-end">
        <Button type="button" onClick={onClose}>{t('detail.close')}</Button>
      </div>
    </Dialog>
  )
}
