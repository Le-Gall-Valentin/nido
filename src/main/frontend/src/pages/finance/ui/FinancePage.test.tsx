import { describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor, fireEvent, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import { SpacesApiProvider } from '@/features/space-switcher'
import type { ISpacesApi } from '@/features/space-switcher'
import { SpaceMembersApiProvider } from '@/entities/space'
import type { IFinanceApi } from '@/entities/finance'
import type { ISpaceMembersApi, SpaceMember, SpaceSummary } from '@/entities/space'
import { FinancePage } from './FinancePage'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

vi.mock('@/features/auth', () => ({ useAuth: vi.fn() }))
import { useAuth } from '@/features/auth'
const mockUseAuth = vi.mocked(useAuth)
mockUseAuth.mockImplementation((selector) => selector({ user: { id: 'u-1' } } as never))

const MEMBERS: SpaceMember[] = [
  { userId: 'u-1', username: 'alice', email: 'a@test.com', role: 'MEMBER', joinedAt: '2024-01-01T00:00:00Z' },
]

const CURRENT_SPACE: SpaceSummary = {
  id: 'space-1', type: 'SHARED', name: 'Chez nous', accent: '#c17a5c', glyph: '🏡', myRole: 'MEMBER', memberCount: 2,
}

function fakeApi(overrides: Partial<IFinanceApi> = {}): IFinanceApi {
  return {
    listCategories: vi.fn().mockResolvedValue([{ id: 'c1', label: 'Alimentation', color: '#f59e0b', icon: 'Utensils', isDefault: true }]),
    createCategory: vi.fn(), updateCategory: vi.fn(), deleteCategory: vi.fn(),
    listBudgets: vi.fn().mockResolvedValue([]), setBudget: vi.fn(), deleteBudget: vi.fn(),
    listTransactions: vi.fn().mockResolvedValue([]), createTransaction: vi.fn(), updateTransaction: vi.fn(), deleteTransaction: vi.fn(),
    listRecurringSeries: vi.fn().mockResolvedValue([]), createRecurringSeries: vi.fn(), updateRecurringSeries: vi.fn(), deleteRecurringSeries: vi.fn(),
    getStats: vi.fn().mockResolvedValue({ balance: 0, totalExpense: 0, totalIncome: 0, remainingBudget: 0, breakdown: [], budgetVsActual: [] }),
    getProjection: vi.fn().mockResolvedValue({ actualBalanceSoFar: 0, upcoming: [], projectedEndOfMonthBalance: 0 }),
    getBalances: vi.fn().mockResolvedValue({ netByMember: [], suggestedTransfers: [] }), settleDebt: vi.fn(),
    listSettlements: vi.fn().mockResolvedValue([]),
    listSavingsGoals: vi.fn().mockResolvedValue([]), createSavingsGoal: vi.fn(), updateSavingsGoal: vi.fn(), deleteSavingsGoal: vi.fn(), addSavingsContribution: vi.fn(),
    ...overrides,
  }
}

function fakeMembersApi(): ISpaceMembersApi {
  return { listMembers: vi.fn().mockResolvedValue(MEMBERS) }
}

function fakeSpacesApi(mySpaces: SpaceSummary[] = [CURRENT_SPACE]): ISpacesApi {
  return { listMySpaces: vi.fn().mockResolvedValue(mySpaces), getSpace: vi.fn() }
}

function renderPage(api: IFinanceApi, space: SpaceSummary = CURRENT_SPACE) {
  const queryClient = createTestQueryClient()
  return render(
    <QueryClientProvider client={queryClient}>
      <SpacesApiProvider api={fakeSpacesApi([space])}>
        <SpaceMembersApiProvider api={fakeMembersApi()}>
          <MemoryRouter initialEntries={['/s/space-1/finance']}>
            <Routes>
              <Route path="/s/:spaceId/finance" element={<FinancePage api={api} />} />
            </Routes>
          </MemoryRouter>
        </SpaceMembersApiProvider>
      </SpacesApiProvider>
    </QueryClientProvider>
  )
}

describe('FinancePage', () => {
  it('renders the stat cards once data has loaded', async () => {
    renderPage(fakeApi())

    await waitFor(() => expect(screen.getByText('stats.balance')).toBeDefined())
  })

  it('shows an error state when transactions fail to load', async () => {
    renderPage(fakeApi({ listTransactions: vi.fn().mockRejectedValue(new Error('boom')) }))

    await waitFor(() => expect(screen.getByText('error.load_failed')).toBeDefined())
  })

  it('renders the month-end projection once loaded', async () => {
    renderPage(fakeApi({
      getProjection: vi.fn().mockResolvedValue({
        actualBalanceSoFar: -50, upcoming: [{ seriesId: 's1', label: 'Loyer', amount: 800, type: 'EXPENSE', date: '2026-01-28' }],
        projectedEndOfMonthBalance: -850,
      }),
    }))

    await waitFor(() => expect(screen.getByText(/Loyer/)).toBeDefined())
  })

  it('opens the budget manager modal and sets a budget for a category that does not have one yet', async () => {
    const setBudget = vi.fn()
    renderPage(fakeApi({ setBudget }))

    await waitFor(() => expect(screen.getByText('budget.manage')).toBeDefined())
    fireEvent.click(screen.getByText('budget.manage'))
    fireEvent.click(screen.getByText('budget.set'))
    fireEvent.change(screen.getByRole('spinbutton'), { target: { value: '150' } })
    fireEvent.click(screen.getByText('form.save'))

    await waitFor(() => expect(setBudget).toHaveBeenCalledWith('space-1', 'c1', 150))
  })

  it('edits a recurring series from the manager modal', async () => {
    const updateRecurringSeries = vi.fn()
    renderPage(fakeApi({
      listRecurringSeries: vi.fn().mockResolvedValue([{
        id: 's1', label: 'Loyer', amount: 800, type: 'EXPENSE', categoryId: 'c1', payerId: 'u-1',
        contributors: [], intervalType: 'MONTHLY', intervalCount: 1, anchorDate: '2026-01-01', endDate: null,
      }]),
      updateRecurringSeries,
    }))

    await waitFor(() => expect(screen.getByText('recurring_series.manage')).toBeDefined())
    fireEvent.click(screen.getByText('recurring_series.manage'))
    fireEvent.click(screen.getByLabelText('recurring_series.edit'))
    fireEvent.change(screen.getByLabelText('form.label_label'), { target: { value: 'Loyer révisé' } })
    fireEvent.click(screen.getByText('form.save'))

    await waitFor(() => expect(updateRecurringSeries).toHaveBeenCalledWith(
      'space-1', 's1', 'Loyer révisé', 800, 'EXPENSE', 'c1', 'u-1', [],
      { intervalType: 'MONTHLY', intervalCount: 1, anchorDate: '2026-01-01', endDate: null }
    ))
  })

  it('deletes a recurring series from the manager modal', async () => {
    const deleteRecurringSeries = vi.fn()
    renderPage(fakeApi({
      listRecurringSeries: vi.fn().mockResolvedValue([{
        id: 's1', label: 'Loyer', amount: 800, type: 'EXPENSE', categoryId: 'c1', payerId: 'u-1',
        contributors: [], intervalType: 'MONTHLY', intervalCount: 1, anchorDate: '2026-01-01', endDate: null,
      }]),
      deleteRecurringSeries,
    }))

    await waitFor(() => expect(screen.getByText('recurring_series.manage')).toBeDefined())
    fireEvent.click(screen.getByText('recurring_series.manage'))
    fireEvent.click(screen.getByLabelText('recurring_series.delete'))
    fireEvent.click(screen.getByText('delete_confirm.confirm'))

    await waitFor(() => expect(deleteRecurringSeries).toHaveBeenCalledWith('space-1', 's1'))
  })

  it('shows a tooltip with the category, percentage and amount when hovering a donut segment', async () => {
    renderPage(fakeApi({
      getStats: vi.fn().mockResolvedValue({
        balance: 0, totalExpense: 45.3, totalIncome: 0, remainingBudget: 0,
        breakdown: [{ categoryId: 'c1', amount: 45.3 }], budgetVsActual: [],
      }),
    }))

    await waitFor(() => expect(screen.getByText('Alimentation')).toBeDefined())
    expect(screen.queryByRole('tooltip')).toBeNull()

    fireEvent.mouseEnter(screen.getByRole('img', { name: /Alimentation/ }))
    const tooltip = screen.getByRole('tooltip')
    expect(tooltip.textContent).toContain('Alimentation')
    expect(tooltip.textContent).toContain('100%')
    expect(tooltip.textContent).toContain('45,30')

    fireEvent.mouseLeave(screen.getByRole('img', { name: /Alimentation/ }))
    expect(screen.queryByRole('tooltip')).toBeNull()
  })

  it('opens the full transaction detail, including contributors and their share, when clicking a transaction', async () => {
    renderPage(fakeApi({
      listTransactions: vi.fn().mockResolvedValue([{
        id: 't1', label: 'Courses', amount: 100, type: 'EXPENSE', categoryId: 'c1', date: '2026-01-15',
        payerId: 'u-1', contributors: [{ memberId: 'u-1', shareAmount: 100 }], recurring: false,
      }]),
    }))

    await waitFor(() => expect(screen.getByText('Courses')).toBeDefined())
    fireEvent.click(screen.getByText('Courses'))

    await waitFor(() => expect(screen.getByText('transactions.detail_title')).toBeDefined())
    expect(screen.getByText('100%')).toBeDefined()
  })

  it('opens the category transactions, then the full detail of one, from the breakdown legend', async () => {
    renderPage(fakeApi({
      getStats: vi.fn().mockResolvedValue({
        balance: 0, totalExpense: 45.3, totalIncome: 0, remainingBudget: 0,
        breakdown: [{ categoryId: 'c1', amount: 45.3 }], budgetVsActual: [],
      }),
      listTransactions: vi.fn().mockResolvedValue([{
        id: 't1', label: 'Courses', amount: 45.3, type: 'EXPENSE', categoryId: 'c1', date: '2026-01-15',
        payerId: 'u-1', contributors: [], recurring: false,
      }]),
    }))

    await waitFor(() => expect(screen.getAllByText('Alimentation').length).toBeGreaterThan(0))
    fireEvent.click(screen.getAllByText('Alimentation')[0])

    const categoryModal = await waitFor(() => screen.getByRole('dialog', { name: /Alimentation/ }))
    fireEvent.click(within(categoryModal).getByText('Courses'))

    await waitFor(() => expect(screen.getByText('transactions.detail_title')).toBeDefined())
  })

  it('only offers to settle a debt the current user is a party to', async () => {
    renderPage(fakeApi({
      getBalances: vi.fn().mockResolvedValue({
        netByMember: [],
        suggestedTransfers: [
          { fromMemberId: 'u-1', toMemberId: 'u-2', amount: 100 },
          { fromMemberId: 'u-2', toMemberId: 'u-3', amount: 50 },
        ],
      }),
    }))

    await waitFor(() => expect(screen.getAllByText(/→/).length).toBe(2))
    expect(screen.getAllByText('balances.settle')).toHaveLength(1)
  })

  it('records a partial settlement for less than the full debt', async () => {
    const settleDebt = vi.fn()
    renderPage(fakeApi({
      getBalances: vi.fn().mockResolvedValue({
        netByMember: [],
        suggestedTransfers: [{ fromMemberId: 'u-1', toMemberId: 'u-2', amount: 500 }],
      }),
      settleDebt,
    }))

    await waitFor(() => expect(screen.getByText('balances.settle')).toBeDefined())
    fireEvent.click(screen.getByText('balances.settle'))
    fireEvent.change(screen.getByLabelText('balances.settle_amount_label'), { target: { value: '250' } })
    fireEvent.click(screen.getByText('balances.settle_confirm'))

    await waitFor(() => expect(settleDebt).toHaveBeenCalledWith('space-1', 'u-1', 'u-2', 250, expect.any(String)))
  })

  it('opens a member\'s payments, then the full detail of one, from the balances list', async () => {
    renderPage(fakeApi({
      getBalances: vi.fn().mockResolvedValue({
        netByMember: [{ memberId: 'u-1', net: 100 }],
        suggestedTransfers: [],
      }),
      listTransactions: vi.fn().mockResolvedValue([{
        id: 't1', label: 'Salaire', amount: 1000, type: 'INCOME', categoryId: 'c1', date: '2026-01-05',
        payerId: 'u-1', contributors: [], recurring: false,
      }]),
    }))

    await waitFor(() => expect(screen.getAllByText('alice').length).toBeGreaterThan(0))
    fireEvent.click(screen.getAllByText('alice')[0])

    const memberModal = await waitFor(() => screen.getByRole('dialog', { name: 'alice' }))
    fireEvent.click(within(memberModal).getByText('Salaire'))

    await waitFor(() => expect(screen.getByText('transactions.detail_title')).toBeDefined())
    // Every Dialog shares the same z-index, so with two open at once the later DOM sibling
    // paints on top — the transaction detail must be the last dialog rendered, or it would
    // be visually hidden behind the member modal that opened it (a real regression once).
    const dialogs = screen.getAllByRole('dialog')
    expect(dialogs[dialogs.length - 1]).toBe(screen.getByRole('dialog', { name: 'transactions.detail_title' }))
  })

  it('opens the settlement history between two members when clicking a debt', async () => {
    const listSettlements = vi.fn().mockResolvedValue([
      { id: 's1', fromMemberId: 'u-1', toMemberId: 'u-2', amount: 20, date: '2026-01-02' },
    ])
    renderPage(fakeApi({
      getBalances: vi.fn().mockResolvedValue({
        netByMember: [],
        suggestedTransfers: [{ fromMemberId: 'u-1', toMemberId: 'u-2', amount: 500 }],
      }),
      listSettlements,
    }))

    await waitFor(() => expect(screen.getByText(/→/)).toBeDefined())
    fireEvent.click(screen.getByText(/→/))

    await waitFor(() => expect(listSettlements).toHaveBeenCalledWith('space-1', 'u-1', 'u-2'))
    await waitFor(() => expect(screen.getByText(/20,00/)).toBeDefined())
  })

  it('shows the manage-categories button to a member who can write', async () => {
    renderPage(fakeApi())

    await waitFor(() => expect(screen.getByText('stats.balance')).toBeDefined())
    expect(screen.getByText('categories.manage')).toBeDefined()
  })

  it('hides the manage-categories button from a read-only viewer, since every action inside it requires write access', async () => {
    renderPage(fakeApi(), { ...CURRENT_SPACE, myRole: 'VIEWER' })

    await waitFor(() => expect(screen.getByText('stats.balance')).toBeDefined())
    expect(screen.queryByText('categories.manage')).toBeNull()
  })

  it('asks for confirmation before deleting a savings goal, and only deletes it once confirmed', async () => {
    const deleteSavingsGoal = vi.fn()
    renderPage(fakeApi({
      listSavingsGoals: vi.fn().mockResolvedValue([
        { id: 'g1', name: 'Vacances', targetAmount: 2000, targetDate: null, color: '#5c7a58', glyph: '🎯', totalContributed: 0, contributions: [] },
      ]),
      deleteSavingsGoal,
    }))

    await waitFor(() => expect(screen.getByText('Vacances')).toBeDefined())
    fireEvent.click(screen.getByLabelText('savings.delete'))

    expect(deleteSavingsGoal).not.toHaveBeenCalled()
    expect(screen.getByText('delete_confirm.message')).toBeDefined()

    fireEvent.click(screen.getByText('delete_confirm.confirm'))

    await waitFor(() => expect(deleteSavingsGoal).toHaveBeenCalledWith('space-1', 'g1'))
  })

  it('asks for confirmation before deleting a category, and only deletes it once confirmed', async () => {
    const deleteCategory = vi.fn()
    renderPage(fakeApi({ deleteCategory }))

    await waitFor(() => expect(screen.getByText('stats.balance')).toBeDefined())
    fireEvent.click(screen.getByText('categories.manage'))
    fireEvent.click(screen.getByRole('button', { name: 'categories.delete' }))

    expect(deleteCategory).not.toHaveBeenCalled()
    expect(screen.getByText('delete_confirm.message')).toBeDefined()

    fireEvent.click(screen.getByText('delete_confirm.confirm'))

    await waitFor(() => expect(deleteCategory).toHaveBeenCalledWith('space-1', 'c1'))
  })

  it('shows an error in the budget modal when the backend rejects the save, instead of failing silently', async () => {
    const setBudget = vi.fn().mockRejectedValue(new Error('boom'))
    renderPage(fakeApi({ setBudget }))

    await waitFor(() => expect(screen.getByText('budget.manage')).toBeDefined())
    fireEvent.click(screen.getByText('budget.manage'))
    fireEvent.click(screen.getByText('budget.set'))
    fireEvent.change(screen.getByRole('spinbutton'), { target: { value: '150' } })
    fireEvent.click(screen.getByText('form.save'))

    await waitFor(() => expect(screen.getByText('form.submit_error')).toBeDefined())
  })
})
