import { screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import type { Dashboard, DashboardCards } from '@/entities/dashboard'
import { DashboardBoard } from './DashboardBoard'
import { renderWithActions } from './cardTestHarness'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

const NOW = { date: '2026-09-26', time: '14:32' }

const ALL: DashboardCards = {
  agenda: { status: 'OK', data: { allDay: [], timed: [], dueToday: [], tomorrow: null } },
  menu: { status: 'OK', data: { today: [], tomorrow: [], unplannedDays: [] } },
  finance: { status: 'OK', data: { month: '2026-09', balance: 0, totalExpense: 0, totalIncome: 0, remainingBudget: 0, budgetsToWatch: [], upcoming: [], balances: null } },
  tasks: { status: 'OK', data: { overdue: [], thisWeek: [], inProgress: [{ id: 't-1', title: 'Trier la cave', dueDate: null, priority: 'LOW', status: 'DOING', assigneeIds: [], subtasksDone: 0, subtasksTotal: 0, recurring: false }], openCount: 1, openCountMine: 1 } },
  shopping: { status: 'OK', data: { remaining: 1, categories: [{ categoryId: 'c-1', name: 'Épicerie', count: 1, preview: ['Café'] }] } },
  savings: { status: 'OK', data: { goals: [] } },
}

function dashboard(cards: DashboardCards, attention: Dashboard['attention'] = []): Dashboard {
  return { date: '2026-09-26', spaceType: 'SHARED', canWrite: true, complete: true, attention, cards }
}

function layouts(container: HTMLElement) {
  return [...container.querySelectorAll('[data-layout]')].map((el) => el.getAttribute('data-layout'))
}

describe('DashboardBoard', () => {
  it('lays out today beside the menu, then finances, then tasks beside shopping and savings', () => {
    const { container } = renderWithActions(<DashboardBoard dashboard={dashboard(ALL)} now={NOW} />)

    expect(layouts(container)).toEqual(['pair', 'tasks-with-side'])
    const titles = screen.getAllByRole('heading', { level: 2 }).map((h) => h.textContent)
    expect(titles).toEqual(['today.title', 'menu.title', 'finance.title', 'tasks.title', 'shopping.title', 'savings.title'])
  })

  it('shows "À traiter" first when something needs an action, and not at all otherwise', () => {
    const { unmount } = renderWithActions(<DashboardBoard dashboard={dashboard(ALL, [{ kind: 'DEBT', severity: 'MEDIUM', toMemberId: 'u-cam', amount: 10 }])} now={NOW} />)
    expect(screen.getAllByRole('heading', { level: 2 })[0].textContent).toBe('attention.title')
    unmount()

    renderWithActions(<DashboardBoard dashboard={dashboard(ALL)} now={NOW} />)
    expect(screen.queryByText('attention.title')).toBeNull()
  })

  it('gives tasks the full width when there is nothing beside them', () => {
    const { container } = renderWithActions(<DashboardBoard dashboard={dashboard({ ...ALL, shopping: null, savings: undefined })} now={NOW} />)
    expect(layouts(container)).toEqual(['pair', 'tasks-only'])
  })

  it('sets shopping and savings side by side when there is no task to show', () => {
    const { container } = renderWithActions(<DashboardBoard dashboard={dashboard({ ...ALL, tasks: null })} now={NOW} />)
    expect(layouts(container)).toEqual(['pair', 'side-only'])
  })

  it('drops the row when neither tasks, shopping nor savings has anything', () => {
    const { container } = renderWithActions(<DashboardBoard dashboard={dashboard({ ...ALL, tasks: null, shopping: null, savings: null })} now={NOW} />)
    expect(layouts(container)).toEqual(['pair'])
  })

  it('keeps an unavailable block in its place, under its own title', () => {
    renderWithActions(<DashboardBoard dashboard={dashboard({ ...ALL, finance: { status: 'UNAVAILABLE' }, menu: { status: 'UNAVAILABLE' } })} now={NOW} />)

    expect(screen.getAllByText('card.unavailable')).toHaveLength(2)
    expect(screen.getByRole('region', { name: 'finance.title' })).toBeDefined()
    expect(screen.getByRole('region', { name: 'tasks.title' })).toBeDefined()
  })
})
