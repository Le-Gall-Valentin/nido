import { useTranslation } from 'react-i18next'
import type { SpaceMember } from '@/entities/space'
import { useCreateTask, useCreateRecurringTask, useUpdateTask, type Task } from '@/entities/tasks'
import type { TaskFormInput } from '../model/types'
import { TaskFormModal } from './TaskFormModal'

interface TaskFormPanelProps {
  spaceId: string
  /** The task being edited, or null when creating one. */
  task: Task | null
  members: SpaceMember[]
  isPersonal: boolean
  onClose: () => void
}

/**
 * Writing a task: which of the three calls a submission turns into, and what to say when one fails.
 *
 * <p>Three mutations serve one form — a task, a recurring task, an edit — and which one runs is decided
 * by the shape of what was submitted, not by the caller. Keeping them next to the form is what lets the
 * page open it (from the header, from a card) without knowing any of that, and lets the pending and
 * error state be read straight off the mutation that is actually running.
 */
export function TaskFormPanel({ spaceId, task, members, isPersonal, onClose }: TaskFormPanelProps) {
  const { t } = useTranslation('tasks')
  const createTask = useCreateTask(spaceId)
  const createRecurringTask = useCreateRecurringTask(spaceId)
  const updateTask = useUpdateTask(spaceId)

  function handleSubmit(input: TaskFormInput) {
    if (task) {
      updateTask.mutate(
        { taskId: task.id, title: input.title, priority: input.priority, dueDate: input.dueDate, assigneeIds: input.assigneeIds },
        { onSuccess: onClose }
      )
      return
    }
    if (input.recurrence) {
      createRecurringTask.mutate(
        { title: input.title, priority: input.priority, subtasks: input.subtasks, recurrence: input.recurrence },
        { onSuccess: onClose }
      )
      return
    }
    createTask.mutate(
      { title: input.title, priority: input.priority, dueDate: input.dueDate, assigneeIds: input.assigneeIds, subtasks: input.subtasks },
      { onSuccess: onClose }
    )
  }

  const failed = createTask.isError || createRecurringTask.isError || updateTask.isError

  return (
    <TaskFormModal
      open
      onClose={onClose}
      onSubmit={handleSubmit}
      initialTask={task}
      members={members}
      isPersonal={isPersonal}
      submitError={failed ? t('form.submit_error') : null}
    />
  )
}
