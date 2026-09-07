export type TaskStatus = 'TODO' | 'DOING' | 'DONE'
export type TaskPriority = 'HIGH' | 'MED' | 'LOW'
export type RecurrenceInterval = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY'

export interface Subtask {
  id: string
  text: string
  done: boolean
}

export interface Task {
  id: string
  title: string
  status: TaskStatus
  priority: TaskPriority
  /** ISO date (YYYY-MM-DD). The backend omits it once the task is DONE. */
  dueDate: string | null
  assigneeIds: string[]
  subtasks: Subtask[]
  recurring: boolean
  /** Set only when `recurring` is true — the series this occurrence was materialized from. */
  recurringSeriesId: string | null
  /** Null for a task created before creator tracking existed, or whose creator's account was deleted. */
  createdBy: string | null
}

export interface RecurrenceInput {
  intervalType: RecurrenceInterval
  intervalCount: number
  leadIntervalType: RecurrenceInterval
  leadIntervalCount: number
  anchorDate: string
  endDate: string | null
  rotationMemberIds: string[]
}

export interface RecurringTaskSeries {
  id: string
  title: string
  priority: TaskPriority
  subtaskTemplates: string[]
  intervalType: RecurrenceInterval
  intervalCount: number
  leadIntervalType: RecurrenceInterval
  leadIntervalCount: number
  anchorDate: string
  endDate: string | null
  rotationMemberIds: string[]
  /** Null for a series created before creator tracking existed, or whose creator's account was deleted. */
  createdBy: string | null
}
