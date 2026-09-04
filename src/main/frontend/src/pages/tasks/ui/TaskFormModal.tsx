import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Plus, X } from 'lucide-react'
import { Dialog, Button, Input, CTA_BUTTON_STYLE } from '@/shared/ui'
import { UserAvatar } from '@/entities/user'
import type { SpaceMember } from '@/entities/space'
import type { RecurrenceInput, RecurrenceInterval, Task, TaskPriority } from '@/entities/tasks'
import { TASK_PRIORITY_ORDER, TASK_PRIORITY_META } from '../lib/taskPriorityMeta'

const INTERVAL_ORDER: RecurrenceInterval[] = ['DAILY', 'WEEKLY', 'MONTHLY']

export interface TaskFormInput {
  title: string
  priority: TaskPriority
  dueDate: string | null
  assigneeIds: string[]
  subtasks: string[]
  recurrence: RecurrenceInput | null
}

interface TaskFormModalProps {
  open: boolean
  onClose: () => void
  onSubmit: (input: TaskFormInput) => void
  initialTask: Task | null
  members: SpaceMember[]
  isPersonal: boolean
}

interface TaskDraft {
  title: string
  priority: TaskPriority
  dueDate: string
  memberIds: string[]
  subtasks: string[]
  recurring: boolean
  intervalType: RecurrenceInterval
  intervalCount: string
  anchorDate: string
}

function draftFrom(task: Task | null): TaskDraft {
  if (!task) {
    return {
      title: '', priority: 'MED', dueDate: '', memberIds: [], subtasks: [],
      recurring: false, intervalType: 'WEEKLY', intervalCount: '1', anchorDate: '',
    }
  }
  return {
    title: task.title, priority: task.priority, dueDate: task.dueDate ?? '', memberIds: task.assigneeIds, subtasks: [],
    recurring: false, intervalType: 'WEEKLY', intervalCount: '1', anchorDate: '',
  }
}

export function TaskFormModal({ open, onClose, onSubmit, initialTask, members, isPersonal }: TaskFormModalProps) {
  const { t } = useTranslation('tasks')
  const [draft, setDraft] = useState(() => draftFrom(initialTask))
  const [newSubtask, setNewSubtask] = useState('')
  const [error, setError] = useState('')
  const isEditing = initialTask !== null

  function toggleMember(userId: string) {
    setDraft((d) => ({
      ...d,
      // Selection order is kept as-is: it's the rotation order for a
      // recurring task's members, and order doesn't matter for a one-off
      // task's simultaneous co-assignees.
      memberIds: d.memberIds.includes(userId) ? d.memberIds.filter((id) => id !== userId) : [...d.memberIds, userId],
    }))
  }

  function addSubtask() {
    if (!newSubtask.trim()) return
    setDraft((d) => ({ ...d, subtasks: [...d.subtasks, newSubtask.trim()] }))
    setNewSubtask('')
  }

  function removeSubtask(index: number) {
    setDraft((d) => ({ ...d, subtasks: d.subtasks.filter((_, i) => i !== index) }))
  }

  function handleSave() {
    if (!draft.title.trim()) {
      setError(t('form.title_required'))
      return
    }
    if (!isEditing && draft.recurring && !draft.anchorDate) {
      setError(t('form.recurrence_anchor_date_required'))
      return
    }
    setError('')
    if (isEditing) {
      onSubmit({
        title: draft.title.trim(), priority: draft.priority, dueDate: draft.dueDate || null,
        assigneeIds: draft.memberIds, subtasks: [], recurrence: null,
      })
      return
    }
    if (draft.recurring) {
      onSubmit({
        title: draft.title.trim(), priority: draft.priority, dueDate: null, assigneeIds: [], subtasks: draft.subtasks,
        recurrence: {
          intervalType: draft.intervalType, intervalCount: Number(draft.intervalCount) || 1,
          anchorDate: draft.anchorDate, rotationMemberIds: draft.memberIds,
        },
      })
      return
    }
    onSubmit({
      title: draft.title.trim(), priority: draft.priority, dueDate: draft.dueDate || null,
      assigneeIds: draft.memberIds, subtasks: draft.subtasks, recurrence: null,
    })
  }

  return (
    <Dialog open={open} onClose={onClose} title={t(isEditing ? 'form.edit_title' : 'form.create_title')} maxWidth="max-w-lg">
      <div className="flex flex-col gap-4">
        <Input label={t('form.title_label')} value={draft.title} onChange={(e) => setDraft((d) => ({ ...d, title: e.target.value }))} />

        <div className="flex flex-col gap-1.5">
          <label htmlFor="task-priority" className="text-[13px] font-semibold text-fg-1">{t('form.priority_label')}</label>
          <select
            id="task-priority"
            value={draft.priority}
            onChange={(e) => setDraft((d) => ({ ...d, priority: e.target.value as TaskPriority }))}
            className="rounded-[10px] border-[1.5px] border-border bg-bg-1 px-3.5 py-[11px] text-[14.5px] text-fg-0 outline-none"
          >
            {TASK_PRIORITY_ORDER.map((p) => <option key={p} value={p}>{t(TASK_PRIORITY_META[p].labelKey)}</option>)}
          </select>
        </div>

        {!isEditing && (
          <label className="flex items-center gap-2 text-sm font-semibold text-fg-1">
            <input type="checkbox" checked={draft.recurring} onChange={(e) => setDraft((d) => ({ ...d, recurring: e.target.checked }))} />
            {t('form.recurring_label')}
          </label>
        )}

        {draft.recurring && !isEditing ? (
          <div className="flex flex-col gap-3 rounded-[10px] bg-bg-2 p-3">
            <div className="flex items-end gap-2">
              <Input label={t('form.recurrence_interval_count_label')} type="number" min={1}
                value={draft.intervalCount} className="w-20"
                onChange={(e) => setDraft((d) => ({ ...d, intervalCount: e.target.value }))} />
              <select
                aria-label={t('form.recurrence_interval_type_label')}
                value={draft.intervalType}
                onChange={(e) => setDraft((d) => ({ ...d, intervalType: e.target.value as RecurrenceInterval }))}
                className="rounded-[10px] border-[1.5px] border-border bg-bg-1 px-3.5 py-[11px] text-[14.5px] text-fg-0"
              >
                {INTERVAL_ORDER.map((i) => <option key={i} value={i}>{t(`form.interval.${i}`)}</option>)}
              </select>
            </div>
            <Input label={t('form.recurrence_anchor_date_label')} type="date" value={draft.anchorDate}
              onChange={(e) => setDraft((d) => ({ ...d, anchorDate: e.target.value }))} />
          </div>
        ) : (
          <Input label={t('form.due_date_label')} type="date" value={draft.dueDate}
            onChange={(e) => setDraft((d) => ({ ...d, dueDate: e.target.value }))} />
        )}

        {!isPersonal && (
          <div className="flex flex-col gap-1.5">
            <span className="text-[13px] font-semibold text-fg-1">
              {t(draft.recurring && !isEditing ? 'form.recurrence_rotation_label' : 'form.assignees_label')}
            </span>
            <div className="flex flex-col gap-1">
              {members.map((member) => (
                <button key={member.userId} type="button" onClick={() => toggleMember(member.userId)}
                  className={`flex items-center gap-2 rounded-[9px] p-1.5 text-left text-sm ${draft.memberIds.includes(member.userId) ? 'bg-accent-dim' : 'hover:bg-bg-2'}`}>
                  <UserAvatar username={member.username ?? '?'} role="USER" className="size-6 rounded-full text-[10px]" />
                  <span className="text-fg-1">{member.username ?? member.email}</span>
                  {draft.recurring && !isEditing && draft.memberIds.includes(member.userId) && (
                    <span className="ml-auto text-xs font-semibold text-fg-3">#{draft.memberIds.indexOf(member.userId) + 1}</span>
                  )}
                </button>
              ))}
            </div>
          </div>
        )}

        {!isEditing && (
          <div className="flex flex-col gap-2">
            <span className="text-[13px] font-semibold text-fg-1">{t('form.subtasks_title')}</span>
            {draft.subtasks.map((subtask, index) => (
              <div key={index} className="flex items-center gap-2">
                <span className="flex-1 text-sm text-fg-1">{subtask}</span>
                <button type="button" onClick={() => removeSubtask(index)} aria-label={t('form.remove')} className="p-1.5 text-fg-3 hover:text-status-red">
                  <X className="size-4" />
                </button>
              </div>
            ))}
            <div className="flex items-center gap-2">
              <Input label={t('form.subtask_placeholder')} srOnlyLabel placeholder={t('form.subtask_placeholder')}
                value={newSubtask} onChange={(e) => setNewSubtask(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); addSubtask() } }} />
              <Button type="button" onClick={addSubtask}><Plus className="size-4" /></Button>
            </div>
          </div>
        )}

        {error && <p className="text-sm font-medium text-status-red">{error}</p>}

        <div className="flex justify-end gap-2 pt-2">
          <Button type="button" onClick={onClose}>{t('form.cancel')}</Button>
          <Button type="button" onClick={handleSave} style={CTA_BUTTON_STYLE}>{t('form.save')}</Button>
        </div>
      </div>
    </Dialog>
  )
}
