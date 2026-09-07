import { render, screen, fireEvent, waitFor, within } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import { SpacesApiProvider } from '@/features/space-switcher'
import type { ISpacesApi } from '@/features/space-switcher'
import { SpaceMembersApiProvider } from '@/entities/space'
import type { ISpaceMembersApi, SpaceMember, SpaceSummary } from '@/entities/space'
import { TasksPage } from './TasksPage'
import type { TasksApi, Task } from '@/entities/tasks'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const TASKS: Task[] = [
  { id: 't1', title: 'Prendre RDV', status: 'TODO', priority: 'HIGH', dueDate: null, assigneeIds: [], subtasks: [], recurring: false, recurringSeriesId: null, createdBy: null },
  { id: 't2', title: 'Répondre à Marie', status: 'DOING', priority: 'LOW', dueDate: null, assigneeIds: [], subtasks: [], recurring: false, recurringSeriesId: null, createdBy: null },
]

const MEMBERS: SpaceMember[] = [
  { userId: 'u-1', username: 'alice', email: 'a@test.com', role: 'MEMBER', joinedAt: '2024-01-01T00:00:00Z' },
]

const CURRENT_SPACE: SpaceSummary = {
  id: 'space-1', type: 'SHARED', name: 'Chez nous', accent: '#c17a5c', glyph: '🏡', myRole: 'MEMBER', memberCount: 2,
}

const RECURRING_SERIES = [{ id: 's-1', title: 'Sortir les poubelles', priority: 'MED' as const, subtaskTemplates: [],
  intervalType: 'WEEKLY' as const, intervalCount: 1, leadIntervalType: 'DAILY' as const, leadIntervalCount: 0,
  anchorDate: '2026-01-07', endDate: null, rotationMemberIds: [], createdBy: 'u-1' }]

function fakeApi(overrides: Partial<TasksApi> = {}): TasksApi {
  return {
    listTasks: vi.fn().mockResolvedValue(TASKS),
    createTask: vi.fn(), createRecurringTask: vi.fn(), updateTask: vi.fn(),
    changeTaskStatus: vi.fn().mockResolvedValue(TASKS[0]), toggleSubtask: vi.fn(),
    deleteTask: vi.fn().mockResolvedValue(undefined), moveTask: vi.fn(),
    listRecurringTaskSeries: vi.fn().mockResolvedValue(RECURRING_SERIES),
    updateRecurringTaskSeries: vi.fn().mockResolvedValue(RECURRING_SERIES[0]),
    deleteRecurringTaskSeries: vi.fn().mockResolvedValue(undefined),
    ...overrides,
  }
}

function fakeMembersApi(): ISpaceMembersApi {
  return { listMembers: vi.fn().mockResolvedValue(MEMBERS) }
}

function fakeSpacesApi(mySpaces: SpaceSummary[] = [CURRENT_SPACE]): ISpacesApi {
  return { listMySpaces: vi.fn().mockResolvedValue(mySpaces), getSpace: vi.fn() }
}

function setup(api: TasksApi = fakeApi()) {
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

  it('tapping the task title opens the task detail view', async () => {
    setup()
    await screen.findByText('Prendre RDV')

    fireEvent.click(screen.getByText('Prendre RDV'))

    expect(await screen.findByText('detail.status_label')).toBeDefined()
  })

  it('tapping the priority/due row also opens the task detail view', async () => {
    setup()
    await screen.findByText('Prendre RDV')

    fireEvent.click(screen.getByText('priority.HIGH'))

    expect(await screen.findByText('detail.status_label')).toBeDefined()
  })

  it('tapping the drag handle opens the status picker and changes the status', async () => {
    const { api } = setup()
    await screen.findByText('Prendre RDV')

    fireEvent.click(screen.getByLabelText('change_status:{"title":"Prendre RDV"}'))
    const dialog = screen.getByRole('dialog')
    fireEvent.click(within(dialog).getByText('column.DOING'))

    await waitFor(() => expect(api.changeTaskStatus).toHaveBeenCalledWith('space-1', 't1', 'DOING'))
  })

  it('disables the current column and disables DONE when subtasks are incomplete', async () => {
    const withSubtask: Task = { ...TASKS[0], subtasks: [{ id: 's1', text: 'A', done: false }] }
    const api = fakeApi({ listTasks: vi.fn().mockResolvedValue([withSubtask]) })
    setup(api)
    await screen.findByText('Prendre RDV')

    fireEvent.click(screen.getByLabelText('change_status:{"title":"Prendre RDV"}'))
    const dialog = screen.getByRole('dialog')
    const buttons = within(dialog)
      .getAllByRole('button')
      .filter((button) => button.getAttribute('data-testid') !== 'dialog-close-button')

    expect((buttons[0] as HTMLButtonElement).disabled).toBe(true) // TODO — current status
    expect((buttons[1] as HTMLButtonElement).disabled).toBe(false) // DOING — selectable
    expect((buttons[2] as HTMLButtonElement).disabled).toBe(true) // DONE — blocked by incomplete subtask
  })

  it('a viewer can open the task detail view but has no status-change or write controls', async () => {
    const api = fakeApi()
    const readOnlySpace: SpaceSummary = { ...CURRENT_SPACE, myRole: 'VIEWER' }
    const queryClient = createTestQueryClient()
    render(
      <QueryClientProvider client={queryClient}>
        <SpacesApiProvider api={fakeSpacesApi([readOnlySpace])}>
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
    await screen.findByText('Prendre RDV')

    fireEvent.click(screen.getByText('Prendre RDV'))

    expect(await screen.findByText('detail.status_label')).toBeDefined()
    expect(screen.queryByLabelText('change_status:{"title":"Prendre RDV"}')).toBeNull()
  })

  it('opens the recurring series manager and edits a series', async () => {
    const { api } = setup()
    await screen.findByText('Prendre RDV')

    fireEvent.click(screen.getByText('recurring_series.manage'))
    expect(await screen.findByText('Sortir les poubelles')).toBeDefined()

    fireEvent.click(screen.getByLabelText('recurring_series.edit'))
    fireEvent.change(screen.getByLabelText('form.title_label'), { target: { value: 'Sortir les poubelles et le compost' } })
    fireEvent.click(screen.getByText('form.save'))

    await waitFor(() => expect(api.updateRecurringTaskSeries).toHaveBeenCalledWith(
      'space-1', 's-1', 'Sortir les poubelles et le compost', 'MED', [], {
        intervalType: 'WEEKLY', intervalCount: 1, leadIntervalType: 'DAILY', leadIntervalCount: 0,
        anchorDate: '2026-01-07', endDate: null, rotationMemberIds: [],
      }))
  })

  it('opens a recurring series detail view by tapping its row in the manager', async () => {
    setup()
    await screen.findByText('Prendre RDV')

    fireEvent.click(screen.getByText('recurring_series.manage'))
    expect(await screen.findByText('Sortir les poubelles')).toBeDefined()

    fireEvent.click(screen.getByText('Sortir les poubelles'))

    expect(await screen.findByText('detail.rotation_participants_label')).toBeDefined()
  })

  it('shows a submit error in the task form when creating a task fails', async () => {
    const api = fakeApi({ createTask: vi.fn().mockRejectedValue(new Error('boom')) })
    setup(api)
    await screen.findByText('Prendre RDV')

    fireEvent.click(screen.getByText('new_task'))
    fireEvent.change(screen.getByLabelText('form.title_label'), { target: { value: 'Nouvelle tâche' } })
    fireEvent.click(screen.getByText('form.save'))

    expect(await screen.findByText('form.submit_error')).toBeDefined()
  })

  it('deletes a recurring series through the confirmation modal', async () => {
    const { api } = setup()
    await screen.findByText('Prendre RDV')

    fireEvent.click(screen.getByText('recurring_series.manage'))
    await screen.findByText('Sortir les poubelles')
    fireEvent.click(screen.getByLabelText('recurring_series.delete'))
    fireEvent.click(screen.getByText('delete_confirm.confirm'))

    await waitFor(() => expect(api.deleteRecurringTaskSeries).toHaveBeenCalledWith('space-1', 's-1'))
  })
})
