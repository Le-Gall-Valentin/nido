import { useQuery } from '@tanstack/react-query'
import { useTasksApi, useRecurringTaskSeriesApi } from './tasksApiContext'

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

export function recurringTaskSeriesKey(spaceId: string) {
  return ['tasks', spaceId, 'recurring-series'] as const
}

export function useRecurringTaskSeries(spaceId: string | undefined) {
  const api = useRecurringTaskSeriesApi()
  return useQuery({
    queryKey: recurringTaskSeriesKey(spaceId ?? ''),
    queryFn: () => api.listRecurringTaskSeries(spaceId as string),
    enabled: !!spaceId,
  })
}
