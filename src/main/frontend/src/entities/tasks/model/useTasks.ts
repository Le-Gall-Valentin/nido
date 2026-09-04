import { useQuery } from '@tanstack/react-query'
import { useTasksApi } from './tasksApiContext'

export function tasksKey(spaceId: string) {
  return ['tasks', spaceId, 'list'] as const
}

export function useTasks(spaceId: string | undefined) {
  const api = useTasksApi()
  return useQuery({
    queryKey: tasksKey(spaceId ?? ''),
    queryFn: () => api.listTasks(spaceId as string),
    enabled: !!spaceId,
  })
}
