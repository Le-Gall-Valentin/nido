/**
 * "Mine" is what is assigned to me, or to nobody — an unassigned chore is anyone's, so it is mine too.
 * The same rule the server applies to overdue tasks in "À traiter".
 */
export function isMine(task: { assigneeIds: string[] }, userId: string | null): boolean {
  return task.assigneeIds.length === 0 || (userId !== null && task.assigneeIds.includes(userId))
}

export function filterMine<T extends { assigneeIds: string[] }>(tasks: T[], userId: string | null): T[] {
  return tasks.filter((task) => isMine(task, userId))
}
