import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import { SpacesApiProvider, type ISpacesApi } from '@/features/space-switcher'
import { SpaceMembersApiProvider, type ISpaceMembersApi, type SpaceMember, type SpaceSummary } from '@/entities/space'
import type { CalendarApi, CalendarOccurrence, RecurringEventSeries } from '@/entities/calendar'
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
  members?: SpaceMember[]
} = {}) {
  const calendar = { listOccurrences: vi.fn().mockResolvedValue(feed), listRecurringEventSeries: vi.fn().mockResolvedValue([]), ...apis.calendar } as unknown as CalendarApi
  const finance = { listSavingsGoals: vi.fn().mockResolvedValue([]), listRecurringSeries: vi.fn().mockResolvedValue([]), listCategories: vi.fn().mockResolvedValue([]), ...apis.finance } as unknown as IFinanceApi
  const kitchen = { listRecipes: vi.fn().mockResolvedValue([]), listMenuEntries: vi.fn().mockResolvedValue([]), ...apis.kitchen } as unknown as IKitchenApi
  const spaces: ISpacesApi = { listMySpaces: vi.fn().mockResolvedValue([{ ...SPACE, myRole: apis.role ?? 'MEMBER' }]), getSpace: vi.fn() }
  const members: ISpaceMembersApi = { listMembers: vi.fn().mockResolvedValue(apis.members ?? []) }
  // The app mounts the tasks API above every page; dropping a task writes through it.
  const tasks = { listTasks: vi.fn().mockResolvedValue([]), listRecurringTaskSeries: vi.fn().mockResolvedValue([]), updateTask: vi.fn() } as unknown as TasksApi
  render(
    <QueryClientProvider client={createTestQueryClient()}>
      <SpacesApiProvider api={spaces}>
        <SpaceMembersApiProvider api={members}>
          <TasksApiProvider api={tasks}>
          <MemoryRouter initialEntries={[`/s/space-1/organisation/calendar?view=${apis.view ?? 'month'}&date=2026-09-23`]}>
            <Routes>
              <Route path="/s/:spaceId/organisation/calendar"
                element={<CalendarPage api={calendar} financeApi={finance} kitchenApi={kitchen} />} />
              <Route path="/s/:spaceId/organisation/tasks" element={<p>tasks page</p>} />
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

  it('opens the tasks page from a task still to come', async () => {
    renderPage([occurrence({
      title: 'Poubelles', source: 'TASK', sourceId: 's-9:2026-09-23', seriesId: 's-9', originalDate: '2026-09-23',
      materialized: false,
    })])
    fireEvent.click(await screen.findByText('Poubelles'))
    fireEvent.click(await screen.findByRole('button', { name: 'detail.open_in.TASK' }))
    expect(await screen.findByText('tasks page')).toBeTruthy()
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

  it('picks the creator as a participant by default when adding an event in a shared context', async () => {
    renderPage([], { members: [
      { userId: 'u-1', username: 'alice', email: null, role: 'OWNER', joinedAt: '2026-01-01T00:00:00Z' },
      { userId: 'u-2', username: 'bob', email: null, role: 'MEMBER', joinedAt: '2026-01-01T00:00:00Z' },
    ] })
    fireEvent.click(await screen.findByRole('button', { name: 'new_event' }))
    const pick = async (name: string) => (await screen.findByRole('button', { name })).getAttribute('aria-pressed')
    expect(await pick('alice')).toBe('true')
    expect(await pick('bob')).toBe('false')
  })

})

/** A weekly series and one of its occurrences, as the feed projects it on the page's day. */
const PIANO_SERIES: RecurringEventSeries = {
  id: 's-1', title: 'Piano', description: 'Salle 3', location: 'Conservatoire', allDay: false,
  startTime: '18:00', endTime: '19:00', durationDays: 0, color: null,
  intervalType: 'MONTHLY', intervalCount: 1, anchorDate: '2026-09-02', endDate: null,
  startsOn: null, firstDate: '2026-09-02',
  participantIds: [], createdBy: 'u-1', createdAt: '2026-01-01T00:00:00Z',
}
const PIANO_OCCURRENCE = occurrence({
  title: 'Piano', sourceId: 's-1:2026-09-23', seriesId: 's-1', originalDate: '2026-09-23', materialized: false,
  allDay: false, startTime: '18:00', endTime: '19:00',
})

const value = (label: string) => (screen.getByLabelText(label) as HTMLInputElement).value

describe('CalendarPage — recurring events in the event form', () => {
  it('starts a series from the new-event button, on the day the calendar shows', async () => {
    const createRecurringEventSeries = vi.fn().mockResolvedValue(PIANO_SERIES)
    const createEvent = vi.fn()
    renderPage([], { calendar: { createRecurringEventSeries, createEvent } })
    fireEvent.click(await screen.findByRole('button', { name: 'new_event' }))
    fireEvent.change(screen.getByLabelText('form.title'), { target: { value: 'Piano' } })
    fireEvent.change(screen.getByLabelText('form.location'), { target: { value: 'Conservatoire' } })
    fireEvent.click(screen.getByLabelText('form.recurring'))
    fireEvent.click(screen.getByRole('button', { name: 'form.save' }))

    await vi.waitFor(() => expect(createRecurringEventSeries).toHaveBeenCalledWith('space-1', expect.objectContaining({
      title: 'Piano', location: 'Conservatoire', allDay: true, anchorDate: '2026-09-23', durationDays: 0,
      intervalType: 'WEEKLY', intervalCount: 1, endDate: null,
    })))
    expect(createEvent).not.toHaveBeenCalled()
  })

  it('turns a time picked out in the week grid into a series at that time', async () => {
    const createRecurringEventSeries = vi.fn().mockResolvedValue(PIANO_SERIES)
    renderPage([], { view: 'week', calendar: { createRecurringEventSeries } })
    const column = await vi.waitFor(() => {
      const found = document.querySelectorAll<HTMLElement>('[data-testid="hour-column"]')[1]
      if (!found) throw new Error('no grid yet')
      return found
    })
    await vi.waitFor(() => {
      fireEvent.pointerDown(column, { pointerType: 'mouse', button: 0, clientY: 600 })
      fireEvent.pointerUp(window, { pointerType: 'mouse', button: 0, clientY: 600 })
      expect(screen.getByRole('dialog')).toBeTruthy()
    })
    fireEvent.change(screen.getByLabelText('form.title'), { target: { value: 'Piano' } })
    fireEvent.click(screen.getByLabelText('form.recurring'))
    fireEvent.click(screen.getByRole('button', { name: 'form.save' }))

    await vi.waitFor(() => expect(createRecurringEventSeries).toHaveBeenCalledWith('space-1', expect.objectContaining({
      allDay: false, anchorDate: '2026-09-22', startTime: '10:00', endTime: '11:00', durationDays: 0,
    })))
  })

  it('edits the whole series from one of its occurrences in the same form, place and description included', async () => {
    const updateRecurringEventSeries = vi.fn().mockResolvedValue(PIANO_SERIES)
    renderPage([PIANO_OCCURRENCE], { calendar: {
      listRecurringEventSeries: vi.fn().mockResolvedValue([PIANO_SERIES]), updateRecurringEventSeries,
    } })
    fireEvent.click(await screen.findByText('Piano'))
    fireEvent.click(await screen.findByRole('button', { name: 'detail.edit' }))
    fireEvent.click(await screen.findByRole('button', { name: 'scope.whole_series' }))

    expect(await screen.findByText('series.edit_title')).toBeTruthy()
    // Begun on September 2 and never ending: the edit carries on from today, and says so.
    expect(screen.getByText('series.split_notice')).toBeTruthy()
    expect(value('form.description')).toBe('Salle 3')
    expect(value('form.location')).toBe('Conservatoire')
    expect(value('form.start_date')).toBe('2026-09-02')
    expect(value('series.interval_type')).toBe('MONTHLY')
    expect(screen.queryByLabelText('form.recurring')).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'form.save' }))

    await vi.waitFor(() => expect(updateRecurringEventSeries).toHaveBeenCalledWith('space-1', 's-1', expect.objectContaining({
      description: 'Salle 3', location: 'Conservatoire', anchorDate: '2026-09-02', intervalType: 'MONTHLY',
    })))
  })

  it('deletes the whole series from one of its occurrences without a detour through the list', async () => {
    const deleteRecurringEventSeries = vi.fn().mockResolvedValue(undefined)
    renderPage([PIANO_OCCURRENCE], { calendar: {
      listRecurringEventSeries: vi.fn().mockResolvedValue([PIANO_SERIES]), deleteRecurringEventSeries,
    } })
    fireEvent.click(await screen.findByText('Piano'))
    fireEvent.click(await screen.findByRole('button', { name: 'detail.delete' }))
    fireEvent.click(await screen.findByRole('button', { name: 'scope.whole_series' }))

    expect(await screen.findByText('series.stop_message')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: 'delete_confirm.confirm' }))

    await vi.waitFor(() => expect(deleteRecurringEventSeries).toHaveBeenCalledWith('space-1', 's-1'))
  })

  it('lists the series to manage them, and edits one in the same form', async () => {
    const ended = { ...PIANO_SERIES, id: 's-0', title: 'Solfège', endDate: '2026-09-01', firstDate: '2026-01-07' }
    renderPage([], { calendar: { listRecurringEventSeries: vi.fn().mockResolvedValue([ended, PIANO_SERIES]) } })
    fireEvent.click(await screen.findByRole('button', { name: 'recurring_series.manage' }))
    const edit = await screen.findByRole('button', { name: 'series.edit:{"name":"Piano"}' })
    // Series are created from the new-event button now, never from here.
    expect(screen.queryByRole('button', { name: /series\.new/ })).toBeNull()
    // Each says from when it shows, and until when for one that ends.
    expect(screen.getByText(/series\.from:\{"date":"2026-01-07"\}/).textContent).toContain('series.until_date:{"date":"2026-09-01"}')
    fireEvent.click(edit)

    expect(await screen.findByText('series.edit_title')).toBeTruthy()
    expect(value('form.description')).toBe('Salle 3')
  })
})
