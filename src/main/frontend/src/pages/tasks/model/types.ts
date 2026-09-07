import type { RecurrenceInput, TaskPriority } from '@/entities/tasks'

/** Shape TaskFormModal submits — for both a one-off task and a new recurring series. */
export interface TaskFormInput {
  title: string
  priority: TaskPriority
  dueDate: string | null
  assigneeIds: string[]
  subtasks: string[]
  recurrence: RecurrenceInput | null
}

/** Shape RecurringTaskSeriesFormModal submits when editing an existing series. */
export interface RecurringTaskSeriesFormInput {
  title: string
  priority: TaskPriority
  subtasks: string[]
  recurrence: RecurrenceInput
}
