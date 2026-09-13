import type { Task, TaskStatus } from '@/entities/tasks'

/**
 * What a drag, a click on a card's checkbox or a pick in the status dialog amounts to.
 *
 * <p>The three cases used to be two — the task, or {@code null} — which put "there is nothing to do"
 * and "the server would refuse this" behind the same answer. Callers could only stay silent, so
 * ticking a task that still had open subtasks did nothing at all and said nothing either. A refusal
 * is the server's own rule ({@code ChangeTaskStatusHandler} answers 409), and the caller has to be
 * able to tell the user which task and how much is left of it.
 */
export type TaskMove =
  | { kind: 'move'; task: Task }
  | { kind: 'nothing' }
  | { kind: 'blocked'; task: Task; openSubtasks: number }

export function resolveTaskMove(tasks: Task[], taskId: string, targetStatus: TaskStatus): TaskMove {
  const task = tasks.find((t) => t.id === taskId)
  if (!task || task.status === targetStatus) return { kind: 'nothing' }
  if (targetStatus === 'DONE') {
    const openSubtasks = task.subtasks.filter((s) => !s.done).length
    if (openSubtasks > 0) return { kind: 'blocked', task, openSubtasks }
  }
  return { kind: 'move', task }
}
