import { renderHook, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import type { ReactNode } from 'react'
import { QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import type { ITasksApi } from './ITasksApi'
import type { Task } from './types'
import { TasksApiProvider } from './tasksApiContext'
import { useTasks, tasksKey } from './useTasks'

const TASKS: Task[] = [{ id: 't-1', title: 'T', status: 'TODO', priority: 'MED', dueDate: null, assigneeIds: [], subtasks: [], recurring: false }]

function fakeApi(overrides: Partial<ITasksApi> = {}): ITasksApi {
  return {
    listTasks: vi.fn().mockResolvedValue(TASKS),
    createTask: vi.fn(), createRecurringTask: vi.fn(), updateTask: vi.fn(),
    changeTaskStatus: vi.fn(), toggleSubtask: vi.fn(), deleteTask: vi.fn(), moveTask: vi.fn(),
    ...overrides,
  }
}

function wrapperFor(api: ITasksApi) {
  const queryClient = createTestQueryClient()
  return ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      <TasksApiProvider api={api}>{children}</TasksApiProvider>
    </QueryClientProvider>
  )
}

describe('useTasks', () => {
  it('builds the ["tasks", spaceId, "list"] key', () => {
    expect(tasksKey('space-1')).toEqual(['tasks', 'space-1', 'list'])
  })

  it('fetches tasks through the injected api', async () => {
    const api = fakeApi()
    const { result } = renderHook(() => useTasks('space-1'), { wrapper: wrapperFor(api) })
    await waitFor(() => expect(result.current.data).toEqual(TASKS))
    expect(api.listTasks).toHaveBeenCalledWith('space-1')
  })

  it('does not run when spaceId is absent', () => {
    const api = fakeApi()
    const { result } = renderHook(() => useTasks(undefined), { wrapper: wrapperFor(api) })
    expect(result.current.fetchStatus).toBe('idle')
    expect(api.listTasks).not.toHaveBeenCalled()
  })
})
