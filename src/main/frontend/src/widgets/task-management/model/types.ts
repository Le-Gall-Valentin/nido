import type { RecurrenceInput, SubtaskEdit, TaskPriority } from '@/entities/tasks'

/** Shape TaskFormModal submits — for both a one-off task and a new recurring series. */
export interface TaskFormInput {
  title: string
  priority: TaskPriority
  dueDate: string | null
  assigneeIds: string[]
  /** In order. Only an edit carries ids — a new task or series has no subtask of its own yet. */
  subtasks: SubtaskEdit[]
  recurrence: RecurrenceInput | null
}

/** Shape RecurringTaskSeriesFormModal submits when editing an existing series. */
export interface RecurringTaskSeriesFormInput {
  title: string
  priority: TaskPriority
  subtasks: string[]
  recurrence: RecurrenceInput
}
