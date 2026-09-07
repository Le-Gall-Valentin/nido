import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Plus, X } from 'lucide-react'
import { Dialog, Button, Input, CTA_BUTTON_STYLE } from '@/shared/ui'
import { UserAvatar } from '@/entities/user'
import type { SpaceMember } from '@/entities/space'
import type { RecurrenceInterval, RecurringTaskSeries, TaskPriority } from '@/entities/tasks'
import type { RecurringTaskSeriesFormInput } from '../model/types'
import { TASK_PRIORITY_ORDER, TASK_PRIORITY_META } from '../lib/taskPriorityMeta'
import { leadTimeExceedsInterval } from './leadTimeExceedsInterval'

const INTERVAL_ORDER: RecurrenceInterval[] = ['DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY']
const SELECT_CLASSNAME = 'flex-1 rounded-[10px] border-[1.5px] border-border bg-bg-1 px-3.5 py-[11px] text-[14.5px] text-fg-0 outline-none'
const PRIORITY_SELECT_CLASSNAME = 'rounded-[10px] border-[1.5px] border-border bg-bg-1 px-3.5 py-[11px] text-[14.5px] text-fg-0 outline-none'

export type { RecurringTaskSeriesFormInput } from '../model/types'

interface RecurringTaskSeriesFormModalProps {
  series: RecurringTaskSeries
  members: SpaceMember[]
  isPersonal: boolean
  onSubmit: (input: RecurringTaskSeriesFormInput) => void
  onCancel: () => void
  /** Set by the caller when the backend rejected the last submission — distinct from the client-side checks below. */
  submitError?: string | null
}

export function RecurringTaskSeriesFormModal({ series, members, isPersonal, onSubmit, onCancel, submitError = null }: RecurringTaskSeriesFormModalProps) {
  const { t } = useTranslation('tasks')
  const [title, setTitle] = useState(series.title)
  const [priority, setPriority] = useState<TaskPriority>(series.priority)
  const [memberIds, setMemberIds] = useState<string[]>(series.rotationMemberIds)
  const [subtasks, setSubtasks] = useState<string[]>(series.subtaskTemplates)
  const [newSubtask, setNewSubtask] = useState('')
  const [intervalType, setIntervalType] = useState<RecurrenceInterval>(series.intervalType)
  const [intervalCount, setIntervalCount] = useState(String(series.intervalCount))
  const [leadIntervalType, setLeadIntervalType] = useState<RecurrenceInterval>(series.leadIntervalType)
  const [leadIntervalCount, setLeadIntervalCount] = useState(String(series.leadIntervalCount))
  const [anchorDate, setAnchorDate] = useState(series.anchorDate)
  const [endDate, setEndDate] = useState(series.endDate ?? '')
  const [error, setError] = useState<string | null>(null)

  function toggleMember(userId: string) {
    setMemberIds((ids) => (ids.includes(userId) ? ids.filter((id) => id !== userId) : [...ids, userId]))
  }

  function addSubtask() {
    if (!newSubtask.trim()) return
    setSubtasks((s) => [...s, newSubtask.trim()])
    setNewSubtask('')
  }

  function removeSubtask(index: number) {
    setSubtasks((s) => s.filter((_, i) => i !== index))
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!title.trim()) {
      setError(t('form.title_required'))
      return
    }
    if (!anchorDate) {
      setError(t('form.recurrence_anchor_date_required'))
      return
    }
    const leadCount = Number(leadIntervalCount) || 0
    const mainCount = Number(intervalCount) || 1
    if (leadTimeExceedsInterval(anchorDate, intervalType, mainCount, leadIntervalType, leadCount)) {
      setError(t('form.lead_time_exceeds_interval'))
      return
    }
    if (endDate && endDate < anchorDate) {
      setError(t('recurring_series.end_date_before_start'))
      return
    }
    setError(null)
    onSubmit({
      title: title.trim(), priority, subtasks,
      recurrence: {
        intervalType, intervalCount: mainCount, leadIntervalType, leadIntervalCount: leadCount,
        anchorDate, endDate: endDate || null, rotationMemberIds: memberIds,
      },
    })
  }

  return (
    <Dialog open onClose={onCancel} title={t('recurring_series.edit_title')} maxWidth="max-w-lg">
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <Input label={t('form.title_label')} value={title} onChange={(e) => setTitle(e.target.value)} />

        <div className="flex flex-col gap-1.5">
          <label htmlFor="series-priority" className="text-[13px] font-semibold text-fg-1">{t('form.priority_label')}</label>
          <select id="series-priority" value={priority} onChange={(e) => setPriority(e.target.value as TaskPriority)} className={PRIORITY_SELECT_CLASSNAME}>
            {TASK_PRIORITY_ORDER.map((p) => <option key={p} value={p}>{t(TASK_PRIORITY_META[p].labelKey)}</option>)}
          </select>
        </div>

        <div className="flex flex-col gap-3 rounded-[10px] bg-bg-2 p-3">
          <div className="flex items-end gap-2">
            <Input label={t('form.recurrence_interval_count_label')} type="number" min={1}
              value={intervalCount} className="w-20" onChange={(e) => setIntervalCount(e.target.value)} />
            <select aria-label={t('form.recurrence_interval_type_label')} value={intervalType}
              onChange={(e) => setIntervalType(e.target.value as RecurrenceInterval)} className={SELECT_CLASSNAME}>
              {INTERVAL_ORDER.map((i) => <option key={i} value={i}>{t(`form.interval.${i}`)}</option>)}
            </select>
          </div>
          <div className="flex items-end gap-2">
            <Input label={t('recurring_series.lead_time_count_label')} type="number" min={0}
              value={leadIntervalCount} className="w-20" onChange={(e) => setLeadIntervalCount(e.target.value)} />
            <select aria-label={t('recurring_series.lead_time_type_label')} value={leadIntervalType}
              onChange={(e) => setLeadIntervalType(e.target.value as RecurrenceInterval)} className={SELECT_CLASSNAME}>
              {INTERVAL_ORDER.map((i) => <option key={i} value={i}>{t(`form.interval.${i}`)}</option>)}
            </select>
          </div>
          <Input label={t('form.recurrence_anchor_date_label')} type="date" value={anchorDate} onChange={(e) => setAnchorDate(e.target.value)} />
          <Input label={t('recurring_series.end_date_label')} type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
        </div>

        {!isPersonal && (
          <div className="flex flex-col gap-1.5">
            <span className="text-[13px] font-semibold text-fg-1">{t('form.recurrence_rotation_label')}</span>
            <div className="flex flex-col gap-1">
              {members.map((member) => (
                <button key={member.userId} type="button" onClick={() => toggleMember(member.userId)}
                  className={`flex items-center gap-2 rounded-[9px] p-1.5 text-left text-sm ${memberIds.includes(member.userId) ? 'bg-accent-dim' : 'hover:bg-bg-2'}`}>
                  <UserAvatar username={member.username ?? '?'} role="USER" className="size-6 rounded-full text-[10px]" />
                  <span className="text-fg-1">{member.username ?? member.email}</span>
                  {memberIds.includes(member.userId) && (
                    <span className="ml-auto text-xs font-semibold text-fg-3">#{memberIds.indexOf(member.userId) + 1}</span>
                  )}
                </button>
              ))}
            </div>
          </div>
        )}

        <div className="flex flex-col gap-2">
          <span className="text-[13px] font-semibold text-fg-1">{t('form.subtasks_title')}</span>
          {subtasks.map((subtask, index) => (
            <div key={index} className="flex items-center gap-2">
              <span className="flex-1 text-sm text-fg-1">{subtask}</span>
              <button type="button" onClick={() => removeSubtask(index)} aria-label={t('form.remove')} className="p-1.5 text-fg-3 hover:text-status-red">
                <X className="size-4" />
              </button>
            </div>
          ))}
          <div className="flex items-center gap-2">
            <div className="flex-1">
              <Input label={t('form.subtask_placeholder')} srOnlyLabel placeholder={t('form.subtask_placeholder')}
                value={newSubtask} onChange={(e) => setNewSubtask(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); addSubtask() } }} />
            </div>
            <Button type="button" onClick={addSubtask}><Plus className="size-4" /></Button>
          </div>
        </div>

        {(error ?? submitError) && <p className="text-sm font-medium text-status-red">{error ?? submitError}</p>}

        <div className="flex gap-2 pt-2">
          <Button type="button" onClick={onCancel} className="flex-1">{t('form.cancel')}</Button>
          <Button type="submit" style={CTA_BUTTON_STYLE} className="flex-1">{t('form.save')}</Button>
        </div>
      </form>
    </Dialog>
  )
}
