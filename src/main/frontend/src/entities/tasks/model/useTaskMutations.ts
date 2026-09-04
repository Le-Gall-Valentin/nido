import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useTasksApi } from './tasksApiContext'
import { tasksKey } from './useTasks'
import type { RecurrenceInput, TaskPriority, TaskStatus } from './types'

export function useCreateTask(spaceId: string) {
  const api = useTasksApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: { title: string; priority: TaskPriority; dueDate: string | null; assigneeIds: string[]; subtasks: string[] }) =>
      api.createTask(spaceId, input.title, input.priority, input.dueDate, input.assigneeIds, input.subtasks),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: tasksKey(spaceId) }),
  })
}

export function useCreateRecurringTask(spaceId: string) {
  const api = useTasksApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: { title: string; priority: TaskPriority; subtasks: string[]; recurrence: RecurrenceInput }) =>
      api.createRecurringTask(spaceId, input.title, input.priority, input.subtasks, input.recurrence),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: tasksKey(spaceId) }),
  })
}

export function useUpdateTask(spaceId: string) {
  const api = useTasksApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: { taskId: string; title: string; priority: TaskPriority; dueDate: string | null; assigneeIds: string[] }) =>
      api.updateTask(spaceId, input.taskId, input.title, input.priority, input.dueDate, input.assigneeIds),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: tasksKey(spaceId) }),
  })
}

export function useChangeTaskStatus(spaceId: string) {
  const api = useTasksApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: { taskId: string; status: TaskStatus }) => api.changeTaskStatus(spaceId, input.taskId, input.status),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: tasksKey(spaceId) }),
  })
}

export function useToggleSubtask(spaceId: string) {
  const api = useTasksApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: { taskId: string; subtaskId: string }) => api.toggleSubtask(spaceId, input.taskId, input.subtaskId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: tasksKey(spaceId) }),
  })
}

export function useDeleteTask(spaceId: string) {
  const api = useTasksApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (taskId: string) => api.deleteTask(spaceId, taskId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: tasksKey(spaceId) }),
  })
}

export function useMoveTask(spaceId: string) {
  const api = useTasksApi()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: { taskId: string; destinationSpaceId: string }) => api.moveTask(spaceId, input.taskId, input.destinationSpaceId),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: tasksKey(spaceId) })
      queryClient.invalidateQueries({ queryKey: tasksKey(variables.destinationSpaceId) })
    },
  })
}
