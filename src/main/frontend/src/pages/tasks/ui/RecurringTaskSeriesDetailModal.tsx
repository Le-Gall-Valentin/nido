import { useTranslation } from 'react-i18next'
import { Dialog, Button } from '@/shared/ui'
import { UserAvatar } from '@/entities/user'
import type { SpaceMember } from '@/entities/space'
import type { RecurringTaskSeries } from '@/entities/tasks'
import { TASK_PRIORITY_META } from '../lib/taskPriorityMeta'

interface RecurringTaskSeriesDetailModalProps {
  series: RecurringTaskSeries
  members: SpaceMember[]
  onClose: () => void
}

export function RecurringTaskSeriesDetailModal({ series, members, onClose }: RecurringTaskSeriesDetailModalProps) {
  const { t } = useTranslation('tasks')
  const priorityMeta = TASK_PRIORITY_META[series.priority]

  function memberLabel(memberId: string): string {
    const member = members.find((m) => m.userId === memberId)
    return member?.username ?? member?.email ?? memberId
  }

  return (
    <Dialog open onClose={onClose} title={t('recurring_series.detail_title')} maxWidth="max-w-lg">
      <div className="mb-4">
        <p className="text-[17px] font-semibold text-fg-0">{series.title}</p>
        <p className={`flex items-center gap-1 text-sm font-semibold ${priorityMeta.textClassName}`}>
          <span className={`size-1.5 rounded-full ${priorityMeta.dotClassName}`} />
          {t(priorityMeta.labelKey)}
        </p>
      </div>

      <dl className="space-y-2.5 text-sm">
        <div className="flex items-center justify-between">
          <dt className="text-fg-3">{t('form.recurrence_interval_type_label')}</dt>
          <dd className="font-medium text-fg-0">
            {t('form.recurrence_interval_count_label')} {series.intervalCount} {t(`form.interval.${series.intervalType}`)}
          </dd>
        </div>
        <div className="flex items-center justify-between">
          <dt className="text-fg-3">{t('recurring_series.lead_time_label')}</dt>
          <dd className="font-medium text-fg-0">
            {series.leadIntervalCount} {t(`form.interval.${series.leadIntervalType}`)} {t('recurring_series.lead_time_before_due_date')}
          </dd>
        </div>
        <div className="flex items-center justify-between">
          <dt className="text-fg-3">{t('form.recurrence_anchor_date_label')}</dt>
          <dd className="font-medium text-fg-0">{series.anchorDate}</dd>
        </div>
        {series.endDate && (
          <div className="flex items-center justify-between">
            <dt className="text-fg-3">{t('recurring_series.end_date_label')}</dt>
            <dd className="font-medium text-fg-0">{series.endDate}</dd>
          </div>
        )}
        <div className="flex items-center justify-between">
          <dt className="text-fg-3">{t('detail.created_by_label')}</dt>
          <dd className="flex items-center gap-1.5 font-medium text-fg-0">
            {series.createdBy ? (
              <>
                <UserAvatar username={memberLabel(series.createdBy)} role="USER" className="size-5 rounded-full text-[9px]" />
                {memberLabel(series.createdBy)}
              </>
            ) : t('detail.unknown_creator')}
          </dd>
        </div>
      </dl>

      {series.subtaskTemplates.length > 0 && (
        <div className="mt-4 border-t border-border pt-4">
          <h3 className="mb-2 text-[13px] font-semibold text-fg-1">{t('form.subtasks_title')}</h3>
          <ul className="space-y-1.5">
            {series.subtaskTemplates.map((text, index) => (
              <li key={index} className="text-sm text-fg-1">{text}</li>
            ))}
          </ul>
        </div>
      )}

      <div className="mt-4 border-t border-border pt-4">
        <h3 className="mb-2 text-[13px] font-semibold text-fg-1">{t('detail.rotation_participants_label')}</h3>
        {series.rotationMemberIds.length > 0 ? (
          <ul className="space-y-1.5">
            {series.rotationMemberIds.map((memberId) => (
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

      <div className="mt-5 flex justify-end">
        <Button type="button" onClick={onClose}>{t('detail.close')}</Button>
      </div>
    </Dialog>
  )
}
