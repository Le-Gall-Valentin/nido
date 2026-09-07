import type { Task, TaskStatus } from '@/entities/tasks'

/**
 * Resolves a drag/drop-or-click move: which task, if any, actually needs its
 * status changed. Returns null (a silent no-op) both when the column is
 * unchanged and when the target is DONE while a subtask is still open — the
 * per-card checkbox stays the one path that surfaces that guard to the user.
 */
export function resolveTaskMove(tasks: Task[], taskId: string, targetStatus: TaskStatus): Task | null {
  const task = tasks.find((t) => t.id === taskId)
  if (!task || task.status === targetStatus) return null
  if (targetStatus === 'DONE' && task.subtasks.some((s) => !s.done)) return null
  return task
}
