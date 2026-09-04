import { renderHook, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import type { ReactNode } from 'react'
import { QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import { TasksApiProvider, type ITasksApi, type Task } from '@/entities/tasks'
import { useOpenTaskCount } from './useOpenTaskCount'

const TASKS: Task[] = [
  { id: 't1', title: 'A', status: 'TODO', priority: 'MED', dueDate: null, assigneeIds: [], subtasks: [], recurring: false },
  { id: 't2', title: 'B', status: 'DOING', priority: 'MED', dueDate: null, assigneeIds: [], subtasks: [], recurring: false },
  { id: 't3', title: 'C', status: 'DONE', priority: 'MED', dueDate: null, assigneeIds: [], subtasks: [], recurring: false },
]

function fakeApi(): ITasksApi {
  return {
    listTasks: vi.fn().mockResolvedValue(TASKS), createTask: vi.fn(), createRecurringTask: vi.fn(), updateTask: vi.fn(),
    changeTaskStatus: vi.fn(), toggleSubtask: vi.fn(), deleteTask: vi.fn(), moveTask: vi.fn(),
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

describe('useOpenTaskCount', () => {
  it('counts only TODO and DOING tasks, never DONE', async () => {
    const { result } = renderHook(() => useOpenTaskCount('space-1'), { wrapper: wrapperFor(fakeApi()) })
    await waitFor(() => expect(result.current).toBe(2))
  })

  it('is zero while the space is unresolved', () => {
    const { result } = renderHook(() => useOpenTaskCount(undefined), { wrapper: wrapperFor(fakeApi()) })
    expect(result.current).toBe(0)
  })
})
