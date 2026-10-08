import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { MemoryRouter, Routes, Route, Link } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import { SpacesApiProvider, type ISpacesApi } from '@/features/space-switcher'
import {
  SpaceApiProvider, SpaceMembersApiProvider, type ISpaceApi, type ISpaceMembersApi, type SpaceMember, type SpaceSummary,
} from '@/entities/space'
import { TasksApiProvider, type TasksApi } from '@/entities/tasks'
import type { IFinanceApi } from '@/entities/finance'
import type { IKitchenApi } from '@/entities/kitchen'
import type { Dashboard, IDashboardApi, TaskItem } from '@/entities/dashboard'
import { DashboardPage } from './DashboardPage'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))
vi.mock('@/features/auth', () => ({ useAuth: vi.fn() }))

import { useAuth } from '@/features/auth'
vi.mocked(useAuth).mockImplementation((selector) => selector({ user: { id: 'u-me', username: 'valentin' } } as never))

const SPACE: SpaceSummary = {
  id: 'space-1', type: 'SHARED', name: 'La Famille', accent: '#c17a5c', glyph: '🏡', myRole: 'MEMBER', memberCount: 3, timezone: 'Europe/Paris',
}
const MEMBERS: SpaceMember[] = [
  { userId: 'u-me', username: 'valentin', email: null, role: 'OWNER', joinedAt: '2026-01-01T00:00:00Z' },
  { userId: 'u-cam', username: 'camille', email: null, role: 'MEMBER', joinedAt: '2026-01-01T00:00:00Z' },
]

const TODAY_TASK: TaskItem = { id: 't-trash', title: 'Sortir les poubelles', dueDate: '2026-09-26', priority: 'MED', status: 'TODO', assigneeIds: ['u-me'], subtasksDone: 0, subtasksTotal: 0, recurring: true, mine: true }
const BLOCKED_TASK: TaskItem = { id: 't-hood', title: 'Changer le filtre', dueDate: '2026-09-22', priority: 'HIGH', status: 'TODO', assigneeIds: [], subtasksDone: 1, subtasksTotal: 3, recurring: false, mine: true }

const BUSY: Dashboard = {
  date: '2026-09-26', spaceType: 'SHARED', canWrite: true, complete: true,
  attention: [
    { kind: 'OVERDUE_TASKS', severity: 'HIGH', count: 1, titles: ['Changer le filtre'] },
    { kind: 'DEBT', severity: 'MEDIUM', toMemberId: 'u-cam', amount: 42.5 },
    { kind: 'INVITATION', severity: 'INFO', invitationId: 'i-1', spaceName: 'Coloc Lyon', spaceGlyph: '🏠', spaceAccent: '#c17a5c', role: 'MEMBER', invitedByUsername: 'camille', expiresAt: '2999-01-01T00:00:00Z' },
  ],
  cards: {
    agenda: { status: 'OK', data: { allDay: [], timed: [], dueToday: [TODAY_TASK], tomorrow: null } },
    menu: { status: 'OK', data: { today: [], tomorrow: [], unplannedDays: [] } },
    finance: { status: 'OK', data: { month: '2026-09', balance: 1240, totalExpense: 1860, totalIncome: 3100, remainingBudget: 340, budgetsToWatch: [], upcoming: [], balances: [] } },
    tasks: { status: 'OK', data: { overdue: [BLOCKED_TASK], thisWeek: [], inProgress: [], openCount: 2, openCountMine: 2 } },
  },
}

interface RenderOptions {
  /** The members list as the page receives it: loaded, still on its way, or failed. */
  members?: SpaceMember[] | Error | 'pending'
}

function renderPage(result: Dashboard | Error = BUSY, { members = MEMBERS }: RenderOptions = {}) {
  const api: IDashboardApi = {
    getDashboard: result instanceof Error ? vi.fn().mockRejectedValue(result) : vi.fn().mockResolvedValue(result),
  }
  const tasksApi = { changeTaskStatus: vi.fn().mockResolvedValue(undefined), listTasks: vi.fn().mockResolvedValue([]) } as unknown as TasksApi
  const spaceApi = { acceptInvitation: vi.fn().mockResolvedValue({ spaceId: 'space-9' }) } as unknown as ISpaceApi
  const financeApi = { settleDebt: vi.fn().mockResolvedValue(undefined) } as unknown as IFinanceApi
  const kitchenApi = { getShoppingList: vi.fn().mockResolvedValue([]) } as unknown as IKitchenApi
  const spacesApi: ISpacesApi = { listMySpaces: vi.fn().mockResolvedValue([SPACE]), getSpace: vi.fn() }
  const membersApi: ISpaceMembersApi = {
    listMembers: members === 'pending' ? vi.fn().mockReturnValue(new Promise(() => {}))
      : members instanceof Error ? vi.fn().mockRejectedValue(members) : vi.fn().mockResolvedValue(members),
  }
  render(
    <QueryClientProvider client={createTestQueryClient()}>
      <SpacesApiProvider api={spacesApi}>
        <SpaceMembersApiProvider api={membersApi}>
          <SpaceApiProvider api={spaceApi}>
            <TasksApiProvider api={tasksApi}>
              <MemoryRouter initialEntries={['/s/space-1/dashboard']}>
                {/* The space switcher's part: the same page, another space. */}
                <Link to="/s/space-2/dashboard">test:other-space</Link>
                <Routes>
                  <Route path="/s/:spaceId/dashboard" element={<DashboardPage api={api} financeApi={financeApi} kitchenApi={kitchenApi} />} />
                </Routes>
              </MemoryRouter>
            </TasksApiProvider>
          </SpaceApiProvider>
        </SpaceMembersApiProvider>
      </SpacesApiProvider>
    </QueryClientProvider>
  )
  return { api, tasksApi, spaceApi }
}

describe('DashboardPage', () => {
  it('shows its skeleton, then the day, the digest and the blocks', async () => {
    renderPage()

    expect(screen.getByRole('status', { name: 'loading' })).toBeDefined()
    expect(await screen.findByRole('heading', { level: 1, name: 'Samedi 26 septembre' })).toBeDefined()
    expect(screen.getByText('hero.digest:{"count":3,"name":"valentin"}')).toBeDefined()
    expect(screen.getByRole('region', { name: 'attention.title' })).toBeDefined()
    expect(screen.getByRole('region', { name: 'today.title' })).toBeDefined()
    expect(screen.getByRole('region', { name: 'finance.title' })).toBeDefined()
    expect(screen.getByRole('region', { name: 'tasks.title' })).toBeDefined()
  })

  it('reads all clear and hides "À traiter" when nothing needs an action', async () => {
    renderPage({ ...BUSY, attention: [] })

    expect(await screen.findByText('hero.all_clear:{"name":"valentin"}')).toBeDefined()
    expect(screen.queryByRole('region', { name: 'attention.title' })).toBeNull()
  })

  it('offers to try again when the dashboard cannot be read at all', async () => {
    const { api } = renderPage(new Error('down'))

    expect(await screen.findByText('error.load_failed')).toBeDefined()
    fireEvent.click(screen.getByRole('button', { name: 'error.retry' }))

    await waitFor(() => expect(api.getDashboard).toHaveBeenCalledTimes(2))
  })

  it('keeps the other blocks when one source failed', async () => {
    renderPage({ ...BUSY, cards: { ...BUSY.cards, finance: { status: 'UNAVAILABLE' } } })

    expect(await screen.findByText('card.unavailable')).toBeDefined()
    expect(screen.getByRole('region', { name: 'tasks.title' })).toBeDefined()
  })

  it('names nobody until the members are known — never a passing "former member"', async () => {
    const { api } = renderPage({ ...BUSY, attention: [{ kind: 'DEBT', severity: 'MEDIUM', toMemberId: 'u-cam', amount: 5 }] }, { members: 'pending' })

    await waitFor(() => expect(api.getDashboard).toHaveBeenCalled())
    await new Promise((resolve) => setTimeout(resolve, 50))
    expect(screen.queryByText(/member.former/)).toBeNull()
    expect(screen.getByRole('status', { name: 'loading' })).toBeDefined()
  })

  it('does not call anyone a former member when the members cannot be read', async () => {
    renderPage({ ...BUSY, attention: [{ kind: 'DEBT', severity: 'MEDIUM', toMemberId: 'u-cam', amount: 5 }] }, { members: new Error('down') })

    expect(await screen.findByText(/attention\.debt:.*"name":"member.generic"/)).toBeDefined()
    expect(screen.queryByText(/member.former/)).toBeNull()
  })

  it('names a member who left as a former member', async () => {
    renderPage({ ...BUSY, attention: [{ kind: 'DEBT', severity: 'MEDIUM', toMemberId: 'u-gone', amount: 5 }] })

    expect(await screen.findByText(/attention\.debt:.*"name":"member.former"/)).toBeDefined()
  })

  it('accepts an invitation and reads the dashboard again', async () => {
    const { api, spaceApi } = renderPage()

    fireEvent.click(await screen.findByRole('button', { name: 'attention.accept' }))

    await waitFor(() => expect(spaceApi.acceptInvitation).toHaveBeenCalledWith('i-1'))
    await waitFor(() => expect(api.getDashboard).toHaveBeenCalledTimes(2))
  })

  it('ticks today\'s task, and refuses the one with open subtasks', async () => {
    const { tasksApi } = renderPage()

    fireEvent.click(await screen.findByRole('checkbox', { name: 'tasks.mark_done:{"title":"Sortir les poubelles"}' }))
    await waitFor(() => expect(tasksApi.changeTaskStatus).toHaveBeenCalledWith('space-1', 't-trash', 'DONE'))

    expect(screen.getByRole('checkbox', { name: 'tasks.mark_done:{"title":"Changer le filtre"}' }).getAttribute('aria-disabled')).toBe('true')
  })

  it('says the day on screen is stale when refreshing it fails, and retries on demand', async () => {
    const { api } = renderPage()
    const box = await screen.findByRole('checkbox', { name: 'tasks.mark_done:{"title":"Sortir les poubelles"}' })
    vi.mocked(api.getDashboard).mockRejectedValueOnce(new Error('down'))

    // A write that succeeds refreshes the page; that refresh is the one that fails.
    fireEvent.click(box)

    expect(await screen.findByText(/^error\.stale:/)).toBeDefined()
    expect(screen.getByRole('checkbox', { name: 'tasks.mark_done:{"title":"Sortir les poubelles"}' })).toBeDefined()

    fireEvent.click(screen.getByRole('button', { name: 'error.retry' }))

    await waitFor(() => expect(screen.queryByText(/^error\.stale:/)).toBeNull())
  })

  it('forgets a failed action when switching to another space', async () => {
    const { api, tasksApi } = renderPage()
    vi.mocked(tasksApi.changeTaskStatus).mockRejectedValueOnce(new Error('down'))

    fireEvent.click(await screen.findByRole('checkbox', { name: 'tasks.mark_done:{"title":"Sortir les poubelles"}' }))
    expect(await screen.findByText('error.action_failed')).toBeDefined()

    fireEvent.click(screen.getByRole('link', { name: 'test:other-space' }))
    // Past the other space's skeleton, which hides everything for a moment.
    await waitFor(() => expect(api.getDashboard).toHaveBeenCalledWith('space-2'))
    expect(await screen.findByRole('checkbox', { name: 'tasks.mark_done:{"title":"Sortir les poubelles"}' })).toBeDefined()
    expect(screen.queryByText('error.action_failed')).toBeNull()
  })

  it('opens the settlement dialog from "À traiter"', async () => {
    renderPage()

    fireEvent.click(await screen.findByRole('button', { name: 'attention.settle' }))

    expect(await screen.findByRole('dialog')).toBeDefined()
  })

  it('opens the task form from "Ajouter"', async () => {
    renderPage()

    fireEvent.click(await screen.findByRole('button', { name: 'hero.add' }))
    fireEvent.click(screen.getByRole('menuitem', { name: 'add_menu.task' }))

    expect(await screen.findByRole('dialog')).toBeDefined()
  })

  it('offers no write to a viewer', async () => {
    renderPage({ ...BUSY, canWrite: false })

    await screen.findByRole('heading', { level: 1 })
    expect(screen.queryByRole('button', { name: 'hero.add' })).toBeNull()
    expect(screen.queryByRole('checkbox')).toBeNull()
    expect(screen.queryByRole('button', { name: 'attention.settle' })).toBeNull()
    expect(screen.getByRole('button', { name: 'attention.accept' })).toBeDefined()
  })
})
