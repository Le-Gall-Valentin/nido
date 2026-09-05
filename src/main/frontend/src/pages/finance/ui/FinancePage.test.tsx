import { describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
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
    listBudgets: vi.fn().mockResolvedValue([]), setBudget: vi.fn(),
    listTransactions: vi.fn().mockResolvedValue([]), createTransaction: vi.fn(), updateTransaction: vi.fn(), deleteTransaction: vi.fn(), moveTransaction: vi.fn(),
    listRecurringSeries: vi.fn().mockResolvedValue([]), createRecurringSeries: vi.fn(), updateRecurringSeries: vi.fn(), deleteRecurringSeries: vi.fn(),
    getStats: vi.fn().mockResolvedValue({ balance: 0, totalExpense: 0, totalIncome: 0, remainingBudget: 0, breakdown: [], budgetVsActual: [] }),
    getProjection: vi.fn().mockResolvedValue({ actualBalanceSoFar: 0, upcoming: [], projectedEndOfMonthBalance: 0 }),
    getBalances: vi.fn().mockResolvedValue({ netByMember: [], suggestedTransfers: [] }), settleDebt: vi.fn(),
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

function renderPage(api: IFinanceApi) {
  const queryClient = createTestQueryClient()
  return render(
    <QueryClientProvider client={queryClient}>
      <SpacesApiProvider api={fakeSpacesApi()}>
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
})
