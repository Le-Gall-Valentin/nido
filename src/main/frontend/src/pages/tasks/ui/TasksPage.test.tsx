import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import { SpacesApiProvider } from '@/features/space-switcher'
import type { ISpacesApi } from '@/features/space-switcher'
import { SpaceMembersApiProvider } from '@/entities/space'
import type { ISpaceMembersApi, SpaceMember, SpaceSummary } from '@/entities/space'
import { TasksPage } from './TasksPage'
import type { ITasksApi, Task } from '@/entities/tasks'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const TASKS: Task[] = [
  { id: 't1', title: 'Prendre RDV', status: 'TODO', priority: 'HIGH', dueDate: null, assigneeIds: [], subtasks: [], recurring: false },
  { id: 't2', title: 'Répondre à Marie', status: 'DOING', priority: 'LOW', dueDate: null, assigneeIds: [], subtasks: [], recurring: false },
]

const MEMBERS: SpaceMember[] = [
  { userId: 'u-1', username: 'alice', email: 'a@test.com', role: 'MEMBER', joinedAt: '2024-01-01T00:00:00Z' },
]

const CURRENT_SPACE: SpaceSummary = {
  id: 'space-1', type: 'SHARED', name: 'Chez nous', accent: '#c17a5c', glyph: '🏡', myRole: 'MEMBER', memberCount: 2,
}

function fakeApi(overrides: Partial<ITasksApi> = {}): ITasksApi {
  return {
    listTasks: vi.fn().mockResolvedValue(TASKS),
    createTask: vi.fn(), createRecurringTask: vi.fn(), updateTask: vi.fn(),
    changeTaskStatus: vi.fn().mockResolvedValue(TASKS[0]), toggleSubtask: vi.fn(),
    deleteTask: vi.fn().mockResolvedValue(undefined), moveTask: vi.fn(),
    ...overrides,
  }
}

function fakeMembersApi(): ISpaceMembersApi {
  return { listMembers: vi.fn().mockResolvedValue(MEMBERS) }
}

function fakeSpacesApi(mySpaces: SpaceSummary[] = [CURRENT_SPACE]): ISpacesApi {
  return { listMySpaces: vi.fn().mockResolvedValue(mySpaces), getSpace: vi.fn() }
}

function setup(api: ITasksApi = fakeApi()) {
  const queryClient = createTestQueryClient()
  render(
    <QueryClientProvider client={queryClient}>
      <SpacesApiProvider api={fakeSpacesApi()}>
        <SpaceMembersApiProvider api={fakeMembersApi()}>
          <MemoryRouter initialEntries={['/s/space-1/organisation/tasks']}>
            <Routes>
              <Route path="/s/:spaceId/organisation/tasks" element={<TasksPage api={api} />} />
            </Routes>
          </MemoryRouter>
        </SpaceMembersApiProvider>
      </SpacesApiProvider>
    </QueryClientProvider>
  )
  return { api }
}

describe('TasksPage', () => {
  it('renders each task under its status column', async () => {
    setup()

    expect(await screen.findByText('Prendre RDV')).toBeDefined()
    expect(screen.getByText('Répondre à Marie')).toBeDefined()
  })

  it('opens the create form and creates a one-off task', async () => {
    const { api } = setup()
    await screen.findByText('Prendre RDV')

    fireEvent.click(screen.getByText('new_task'))
    fireEvent.change(screen.getByLabelText('form.title_label'), { target: { value: 'Nouvelle tâche' } })
    fireEvent.click(screen.getByText('form.save'))

    await waitFor(() => expect(api.createTask).toHaveBeenCalledWith('space-1', 'Nouvelle tâche', 'MED', null, [], []))
  })

  it('marking a task done with an open subtask is a no-op', async () => {
    const withSubtask: Task = { ...TASKS[0], subtasks: [{ id: 's1', text: 'A', done: false }] }
    const api = fakeApi({ listTasks: vi.fn().mockResolvedValue([withSubtask]) })
    setup(api)
    await screen.findByText('Prendre RDV')

    fireEvent.click(screen.getByLabelText('toggle_done:{"title":"Prendre RDV"}'))

    expect(api.changeTaskStatus).not.toHaveBeenCalled()
  })

  it('deletes a task through the confirmation modal', async () => {
    const { api } = setup()
    await screen.findByText('Prendre RDV')

    fireEvent.click(screen.getAllByText('delete')[0])
    fireEvent.click(screen.getByText('delete_confirm.confirm'))

    await waitFor(() => expect(api.deleteTask).toHaveBeenCalledWith('space-1', 't1'))
  })

  it('toggles an individual subtask from the card', async () => {
    const withSubtask: Task = { ...TASKS[0], subtasks: [{ id: 's1', text: 'Comparer les prix', done: false }] }
    const api = fakeApi({ listTasks: vi.fn().mockResolvedValue([withSubtask]) })
    setup(api)
    await screen.findByText('Comparer les prix')

    fireEvent.click(screen.getByText('Comparer les prix'))

    await waitFor(() => expect(api.toggleSubtask).toHaveBeenCalledWith('space-1', 't1', 's1'))
  })
})
