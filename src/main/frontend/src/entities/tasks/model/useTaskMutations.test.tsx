import { renderHook, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import type { ReactNode } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { ITasksApi } from './ITasksApi'
import type { Task } from './types'
import { TasksApiProvider } from './tasksApiContext'
import {
  useCreateTask, useCreateRecurringTask, useUpdateTask, useChangeTaskStatus,
  useToggleSubtask, useDeleteTask, useMoveTask,
} from './useTaskMutations'
import { tasksKey } from './useTasks'

const TASK: Task = { id: 't-1', title: 'T', status: 'TODO', priority: 'MED', dueDate: null, assigneeIds: [], subtasks: [], recurring: false }

function fakeApi(overrides: Partial<ITasksApi> = {}): ITasksApi {
  return {
    listTasks: vi.fn(), createTask: vi.fn().mockResolvedValue(TASK), createRecurringTask: vi.fn().mockResolvedValue(TASK),
    updateTask: vi.fn().mockResolvedValue(TASK), changeTaskStatus: vi.fn().mockResolvedValue(TASK),
    toggleSubtask: vi.fn().mockResolvedValue(undefined), deleteTask: vi.fn().mockResolvedValue(undefined),
    moveTask: vi.fn().mockResolvedValue(TASK),
    ...overrides,
  }
}

function wrapperFor(api: ITasksApi, queryClient: QueryClient) {
  return ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      <TasksApiProvider api={api}>{children}</TasksApiProvider>
    </QueryClientProvider>
  )
}

describe('task mutations', () => {
  it('useCreateTask calls the api and invalidates the list', async () => {
    const api = fakeApi()
    const queryClient = new QueryClient()
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries')
    const { result } = renderHook(() => useCreateTask('space-1'), { wrapper: wrapperFor(api, queryClient) })

    result.current.mutate({ title: 'T', priority: 'MED', dueDate: null, assigneeIds: [], subtasks: [] })

    await waitFor(() => expect(api.createTask).toHaveBeenCalledWith('space-1', 'T', 'MED', null, [], []))
    await waitFor(() => expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: tasksKey('space-1') }))
  })

  it('useCreateRecurringTask calls the api with the recurrence block', async () => {
    const api = fakeApi()
    const queryClient = new QueryClient()
    const { result } = renderHook(() => useCreateRecurringTask('space-1'), { wrapper: wrapperFor(api, queryClient) })
    const recurrence = { intervalType: 'WEEKLY' as const, intervalCount: 1, anchorDate: '2026-01-07', rotationMemberIds: [] }

    result.current.mutate({ title: 'T', priority: 'MED', subtasks: [], recurrence })

    await waitFor(() => expect(api.createRecurringTask).toHaveBeenCalledWith('space-1', 'T', 'MED', [], recurrence))
  })

  it('useUpdateTask calls the api', async () => {
    const api = fakeApi()
    const queryClient = new QueryClient()
    const { result } = renderHook(() => useUpdateTask('space-1'), { wrapper: wrapperFor(api, queryClient) })

    result.current.mutate({ taskId: 't-1', title: 'T2', priority: 'HIGH', dueDate: '2026-01-07', assigneeIds: ['u-1'] })

    await waitFor(() => expect(api.updateTask).toHaveBeenCalledWith('space-1', 't-1', 'T2', 'HIGH', '2026-01-07', ['u-1']))
  })

  it('useChangeTaskStatus calls the api', async () => {
    const api = fakeApi()
    const queryClient = new QueryClient()
    const { result } = renderHook(() => useChangeTaskStatus('space-1'), { wrapper: wrapperFor(api, queryClient) })

    result.current.mutate({ taskId: 't-1', status: 'DOING' })

    await waitFor(() => expect(api.changeTaskStatus).toHaveBeenCalledWith('space-1', 't-1', 'DOING'))
  })

  it('useToggleSubtask calls the api', async () => {
    const api = fakeApi()
    const queryClient = new QueryClient()
    const { result } = renderHook(() => useToggleSubtask('space-1'), { wrapper: wrapperFor(api, queryClient) })

    result.current.mutate({ taskId: 't-1', subtaskId: 'sub-1' })

    await waitFor(() => expect(api.toggleSubtask).toHaveBeenCalledWith('space-1', 't-1', 'sub-1'))
  })

  it('useDeleteTask calls the api', async () => {
    const api = fakeApi()
    const queryClient = new QueryClient()
    const { result } = renderHook(() => useDeleteTask('space-1'), { wrapper: wrapperFor(api, queryClient) })

    result.current.mutate('t-1')

    await waitFor(() => expect(api.deleteTask).toHaveBeenCalledWith('space-1', 't-1'))
  })

  it('useMoveTask calls the api and invalidates both spaces\' lists', async () => {
    const api = fakeApi()
    const queryClient = new QueryClient()
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries')
    const { result } = renderHook(() => useMoveTask('space-1'), { wrapper: wrapperFor(api, queryClient) })

    result.current.mutate({ taskId: 't-1', destinationSpaceId: 'space-2' })

    await waitFor(() => expect(api.moveTask).toHaveBeenCalledWith('space-1', 't-1', 'space-2'))
    await waitFor(() => expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: tasksKey('space-1') }))
    await waitFor(() => expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: tasksKey('space-2') }))
  })
})
