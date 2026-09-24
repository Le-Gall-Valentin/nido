import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import { SpacesApiProvider, type ISpacesApi } from '@/features/space-switcher'
import { SpaceMembersApiProvider, type ISpaceMembersApi, type SpaceSummary } from '@/entities/space'
import type { CalendarApi, CalendarOccurrence } from '@/entities/calendar'
import type { IFinanceApi } from '@/entities/finance'
import type { IKitchenApi } from '@/entities/kitchen'
import { TasksApiProvider, type TasksApi } from '@/entities/tasks'
import { CalendarPage } from './CalendarPage'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, o?: Record<string, unknown>) => (o ? `${k}:${JSON.stringify(o)}` : k) }),
}))

// The page reads the signed-in user to offer "join" or "leave"; the store needs its provider in the
// app, so the harness answers the selector directly — as AdminUsersPage.test.tsx does.
vi.mock('@/features/auth', () => ({
  useAuth: (selector: (state: { user: { id: string } }) => unknown) => selector({ user: { id: 'u-1' } }),
}))

const SPACE: SpaceSummary = {
  id: 'space-1', type: 'SHARED', name: 'Chez nous', accent: '#c17a5c', glyph: '🏡',
  myRole: 'MEMBER', memberCount: 1, timezone: 'Europe/Paris',
}

function occurrence(overrides: Partial<CalendarOccurrence> & { title: string }): CalendarOccurrence {
  return {
    source: 'EVENT', sourceId: overrides.title, seriesId: null, originalDate: null, materialized: true,
    description: null, location: null, allDay: true, startDate: '2026-09-23', startTime: null,
    endDate: '2026-09-23', endTime: null, color: null, participantIds: [], ...overrides,
  }
}

/**
 * The page as the app mounts it, every API stubbed. No test rendered the page before, which is how
 * three of its editors crashed on a missing provider without a single red test.
 */
function renderPage(feed: CalendarOccurrence[], apis: {
  calendar?: Partial<CalendarApi>; finance?: Partial<IFinanceApi>; kitchen?: Partial<IKitchenApi>; role?: SpaceSummary['myRole']
  view?: 'month' | 'week' | 'day'
} = {}) {
  const calendar = { listOccurrences: vi.fn().mockResolvedValue(feed), listRecurringEventSeries: vi.fn().mockResolvedValue([]), ...apis.calendar } as unknown as CalendarApi
  const finance = { listSavingsGoals: vi.fn().mockResolvedValue([]), listRecurringSeries: vi.fn().mockResolvedValue([]), listCategories: vi.fn().mockResolvedValue([]), ...apis.finance } as unknown as IFinanceApi
  const kitchen = { listRecipes: vi.fn().mockResolvedValue([]), listMenuEntries: vi.fn().mockResolvedValue([]), ...apis.kitchen } as unknown as IKitchenApi
  const spaces: ISpacesApi = { listMySpaces: vi.fn().mockResolvedValue([{ ...SPACE, myRole: apis.role ?? 'MEMBER' }]), getSpace: vi.fn() }
  const members: ISpaceMembersApi = { listMembers: vi.fn().mockResolvedValue([]) }
  // The app mounts the tasks API above every page; dropping a task writes through it.
  const tasks = { listTasks: vi.fn().mockResolvedValue([]), updateTask: vi.fn() } as unknown as TasksApi
  render(
    <QueryClientProvider client={createTestQueryClient()}>
      <SpacesApiProvider api={spaces}>
        <SpaceMembersApiProvider api={members}>
          <TasksApiProvider api={tasks}>
          <MemoryRouter initialEntries={[`/s/space-1/organisation/calendar?view=${apis.view ?? 'month'}&date=2026-09-23`]}>
            <Routes>
              <Route path="/s/:spaceId/organisation/calendar"
                element={<CalendarPage api={calendar} financeApi={finance} kitchenApi={kitchen} />} />
            </Routes>
          </MemoryRouter>
          </TasksApiProvider>
        </SpaceMembersApiProvider>
      </SpacesApiProvider>
    </QueryClientProvider>)
  return { calendar, finance, kitchen }
}

describe('CalendarPage', () => {
  it('opens a savings deadline instead of crashing', async () => {
    const { finance } = renderPage([occurrence({ title: 'Vacances', source: 'SAVINGS', sourceId: 'goal-1' })], {
      finance: { listSavingsGoals: vi.fn().mockResolvedValue([
        { id: 'goal-1', name: 'Vacances', targetAmount: 1200, targetDate: '2026-09-23', color: '#5c7a58', glyph: '🎯',
          totalContributed: 0, contributions: [] },
      ]) },
    })
    fireEvent.click(await screen.findByText('Vacances'))
    expect(await screen.findByRole('dialog')).toBeTruthy()
    expect(finance.listSavingsGoals).toHaveBeenCalledWith('space-1')
  })

  it('opens a finance occurrence instead of crashing', async () => {
    const { finance } = renderPage([occurrence({ title: 'Loyer', source: 'FINANCE', sourceId: 't-1', seriesId: 's-1' })])
    fireEvent.click(await screen.findByText('Loyer'))
    expect(await screen.findByRole('dialog')).toBeTruthy()
    expect(finance.listRecurringSeries).toHaveBeenCalledWith('space-1')
  })

  it('opens the meal planner instead of crashing', async () => {
    // The meal's first dialog is read-only and needs no kitchen API; the planner behind its button
    // is what reads recipes, and what crashed.
    const { kitchen } = renderPage([occurrence({ title: 'Gratin', source: 'MEAL', sourceId: 'm-1' })])
    fireEvent.click(await screen.findByText('Gratin'))
    fireEvent.click(await screen.findByText('detail.open_in.MEAL'))
    expect(await screen.findByRole('dialog')).toBeTruthy()
    expect(kitchen.listRecipes).toHaveBeenCalledWith('space-1')
  })

  it('offers a drag handle to a member', async () => {
    renderPage([occurrence({ title: 'Concert' })])
    const chip = (await screen.findByText('Concert')).closest('button')
    expect(chip?.getAttribute('data-draggable')).toBe('true')
  })

  it('offers no drag handle to a viewer', async () => {
    renderPage([occurrence({ title: 'Concert' })], { role: 'VIEWER' })
    const chip = (await screen.findByText('Concert')).closest('button')
    expect(chip?.getAttribute('data-draggable')).toBeNull()
  })

  it('opens the new-event form on the time picked out in the week grid', async () => {
    renderPage([], { view: 'week' })
    // The role resolves asynchronously: the grid only picks once the member is known to write.
    const column = await vi.waitFor(() => {
      const found = document.querySelectorAll<HTMLElement>('[data-testid="hour-column"]')[1]
      if (!found) throw new Error('no grid yet')
      return found
    })
    // A column's top is 0 in jsdom: clientY 600 is 10:00, on Tuesday the 22nd.
    await vi.waitFor(() => {
      fireEvent.pointerDown(column, { pointerType: 'mouse', button: 0, clientY: 600 })
      fireEvent.pointerUp(window, { pointerType: 'mouse', button: 0, clientY: 600 })
      expect(screen.getByRole('dialog')).toBeTruthy()
    })
    const value = (label: string) => (screen.getByLabelText(label) as HTMLInputElement).value
    expect(value('form.start_date')).toBe('2026-09-22')
    expect(value('form.start_time')).toBe('10:00')
    expect(value('form.end_date')).toBe('2026-09-22')
    expect(value('form.end_time')).toBe('11:00')
    expect((screen.getByRole('checkbox', { name: 'form.all_day' }) as HTMLInputElement).checked).toBe(false)
  })
})
