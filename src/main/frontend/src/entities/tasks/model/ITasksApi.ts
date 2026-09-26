import type { RecurrenceInput, SubtaskEdit, Task, TaskPriority, TaskStatus } from './types'

/**
 * Port for task operations. Consumers (hooks) depend on this contract, never
 * on the concrete axios-backed implementation, which is injected through
 * TasksApiProvider. Recurring series management lives in IRecurringTaskSeriesApi
 * instead — a hook that only edits series shouldn't need to know this interface exists.
 */
export interface ITasksApi {
  listTasks(spaceId: string): Promise<Task[]>
  createTask(spaceId: string, title: string, priority: TaskPriority, dueDate: string | null, assigneeIds: string[], subtasks: string[]): Promise<Task>
  createRecurringTask(spaceId: string, title: string, priority: TaskPriority, subtasks: string[], recurrence: RecurrenceInput): Promise<Task>
  /** Leaving `subtasks` out leaves the task's subtasks as they are; an empty list removes them all. */
  updateTask(spaceId: string, taskId: string, title: string, priority: TaskPriority, dueDate: string | null, assigneeIds: string[],
    subtasks?: SubtaskEdit[]): Promise<Task>
  changeTaskStatus(spaceId: string, taskId: string, status: TaskStatus): Promise<Task>
  toggleSubtask(spaceId: string, taskId: string, subtaskId: string): Promise<void>
  deleteTask(spaceId: string, taskId: string): Promise<void>
  moveTask(spaceId: string, taskId: string, destinationSpaceId: string): Promise<Task>
}
