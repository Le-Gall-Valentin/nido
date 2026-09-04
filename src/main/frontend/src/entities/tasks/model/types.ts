export type TaskStatus = 'TODO' | 'DOING' | 'DONE'
export type TaskPriority = 'HIGH' | 'MED' | 'LOW'
export type RecurrenceInterval = 'DAILY' | 'WEEKLY' | 'MONTHLY'

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
}

export interface RecurrenceInput {
  intervalType: RecurrenceInterval
  intervalCount: number
  anchorDate: string
  rotationMemberIds: string[]
}
