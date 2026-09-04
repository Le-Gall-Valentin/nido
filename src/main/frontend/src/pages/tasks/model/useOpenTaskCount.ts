import { useTasks } from '@/entities/tasks'

/** Derives the shell nav badge count from the same cache useTasks populates — no extra request, mirrors useHasPendingInvitations. */
export function useOpenTaskCount(spaceId: string | undefined): number {
  const { data } = useTasks(spaceId)
  return (data ?? []).filter((task) => task.status === 'TODO' || task.status === 'DOING').length
}
