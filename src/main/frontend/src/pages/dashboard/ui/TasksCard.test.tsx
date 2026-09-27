import { fireEvent, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import type { TaskItem, TasksCard as TasksCardData } from '@/entities/dashboard'
import { TasksCard } from './TasksCard'
import { renderWithActions } from '../test/renderWithActions'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

/** `mine` as the server decides it in a shared space — mine or nobody's — unless a test says otherwise. */
function task(id: string, title: string, assigneeIds: string[], extra: Partial<TaskItem> = {}): TaskItem {
  const mine = assigneeIds.length === 0 || assigneeIds.includes('u-me')
  return { id, title, dueDate: '2026-09-29', priority: 'MED', status: 'TODO', assigneeIds, subtasksDone: 0, subtasksTotal: 0, recurring: false, mine, ...extra }
}

const CARD: TasksCardData = {
  overdue: [task('t-hood', 'Changer le filtre de la hotte', ['u-me'], { dueDate: '2026-09-22', priority: 'HIGH' })],
  thisWeek: [task('t-canteen', 'Payer la cantine', []), task('t-plants', 'Arroser les plantes', ['u-cam'])],
  inProgress: [task('t-cellar', 'Trier la cave', ['u-me'], { dueDate: null, status: 'DOING', subtasksDone: 3, subtasksTotal: 5 })],
  openCount: 9,
  openCountMine: 6,
}

describe('TasksCard', () => {
  it('shows as mine what the server says is mine, not what it would guess from the assignees', () => {
    // One rule, the server's (DashboardContext#isMine): the card no longer works it out again.
    const card: TasksCardData = { overdue: [], thisWeek: [task('t-x', 'Décidé par le serveur', [], { mine: false })], inProgress: [], openCount: 1, openCountMine: 0 }

    renderWithActions(<TasksCard card={card} />)

    expect(screen.queryByText('Décidé par le serveur')).toBeNull()
    expect(screen.getByText('tasks.none_mine')).toBeDefined()
  })

  it('does not count as "other" the tasks due today that the agenda already shows', () => {
    const card: TasksCardData = { overdue: [task('t-late', 'En retard', ['u-me'], { dueDate: '2026-09-22' })], thisWeek: [], inProgress: [], openCount: 3, openCountMine: 3 }
    const dueToday = [task('t-bins', 'Sortir les poubelles', ['u-me'], { dueDate: '2026-09-26' }), task('t-mail', 'Poster le courrier', [], { dueDate: '2026-09-26' })]

    renderWithActions(<TasksCard card={card} dueToday={dueToday} />)

    // 3 open = 1 shown here + 2 shown under "Aujourd'hui": nothing else to point at.
    expect(screen.queryByText(/tasks\.more/)).toBeNull()
  })

  it('opens on my tasks in a shared space — mine or nobody\'s — and shows everyone\'s on demand', () => {
    renderWithActions(<TasksCard card={CARD} />)

    expect(screen.getByRole('button', { name: 'tasks.mine' }).getAttribute('aria-pressed')).toBe('true')
    expect(screen.getByText('Payer la cantine')).toBeDefined()
    expect(screen.queryByText('Arroser les plantes')).toBeNull()

    fireEvent.click(screen.getByRole('button', { name: 'tasks.all' }))

    expect(screen.getByText('Arroser les plantes')).toBeDefined()
  })

  it('puts overdue tasks under a red heading, then this week, then in progress', () => {
    renderWithActions(<TasksCard card={CARD} />)

    expect(screen.getByRole('heading', { name: 'tasks.overdue' }).className).toContain('text-status-red')
    expect(screen.getByRole('heading', { name: 'tasks.this_week' })).toBeDefined()
    expect(screen.getByRole('heading', { name: 'tasks.in_progress' })).toBeDefined()
    expect(screen.getByText('tasks.no_due_date')).toBeDefined()
  })

  it('counts the open tasks it does not show and links to the board', () => {
    renderWithActions(<TasksCard card={CARD} />)

    expect(screen.getByRole('link', { name: 'tasks.more:{"count":3}' }).getAttribute('href')).toBe('/s/space-1/organisation/tasks')
    fireEvent.click(screen.getByRole('button', { name: 'tasks.all' }))
    expect(screen.getByRole('link', { name: 'tasks.more:{"count":5}' })).toBeDefined()
  })

  it('has no toggle in a personal space, where every task is mine', () => {
    renderWithActions(<TasksCard card={CARD} />, { actions: { isShared: false } })

    expect(screen.queryByRole('group', { name: 'tasks.filter_label' })).toBeNull()
    expect(screen.getByText('Arroser les plantes')).toBeDefined()
  })

  it('says so when nothing is mine', () => {
    const theirs = { ...CARD, overdue: [], inProgress: [], thisWeek: [task('t-plants', 'Arroser les plantes', ['u-cam'])], openCountMine: 0 }
    renderWithActions(<TasksCard card={theirs} />)

    expect(screen.getByText('tasks.none_mine')).toBeDefined()
  })
})
