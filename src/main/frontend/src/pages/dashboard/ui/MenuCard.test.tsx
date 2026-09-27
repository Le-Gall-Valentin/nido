import { fireEvent, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import type { MenuCard as MenuCardData } from '@/entities/dashboard'
import { MenuCard } from './MenuCard'
import { renderWithActions } from './cardTestHarness'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))
// The real modal mounts its own shopping API and reads categories over HTTP; this test is about when
// the dashboard opens it and with which lines.
vi.mock('@/features/export-menu-to-shopping-list', () => ({
  ExportToShoppingListModal: ({ shoppingList }: { shoppingList: unknown[] }) => <p>export-modal {shoppingList.length}</p>,
}))

const CARD: MenuCardData = {
  today: [
    { entryId: 'm-1', recipeId: 'r-1', recipeName: 'Curry de lentilles corail', category: 'VEGETARIAN', minutes: 35, portions: 4 },
    { entryId: 'm-2', recipeId: null, recipeName: null, category: null, minutes: null, portions: 6 },
  ],
  tomorrow: [{ entryId: 'm-3', recipeId: 'r-3', recipeName: 'Soupe de potiron', category: 'SOUP', minutes: 40, portions: 4 }],
  unplannedDays: ['2026-10-01', '2026-10-02'],
}

describe('MenuCard', () => {
  it('lists today and tomorrow with category, servings and time', () => {
    renderWithActions(<MenuCard card={CARD} date="2026-09-26" />)

    expect(screen.getByText('Curry de lentilles corail')).toBeDefined()
    expect(screen.getByText('menu.category.VEGETARIAN')).toBeDefined()
    expect(screen.getAllByText('menu.portions:{"count":4}')).toHaveLength(2)
    expect(screen.getByText('menu.minutes:{"count":35}')).toBeDefined()
    expect(screen.getByText('menu.tomorrow')).toBeDefined()
    expect(screen.getByText('Soupe de potiron')).toBeDefined()
  })

  it('shows a recipe deleted after it was planned as such, with its servings', () => {
    renderWithActions(<MenuCard card={CARD} date="2026-09-26" />)

    expect(screen.getByText('menu.deleted_recipe')).toBeDefined()
    expect(screen.getByText('menu.portions:{"count":6}')).toBeDefined()
  })

  it('names the days with nothing planned and links to planning them', () => {
    renderWithActions(<MenuCard card={CARD} date="2026-09-26" />)

    expect(screen.getByText(/^menu\.unplanned:.*jeudi.*vendredi/)).toBeDefined()
    expect(screen.getByRole('link', { name: 'menu.plan' }).getAttribute('href')).toBe('/s/space-1/kitchen/menu')
  })

  it('says the whole week is empty, and offers no shopping export for it', () => {
    const empty = { today: [], tomorrow: [], unplannedDays: ['2026-09-26', '2026-09-27', '2026-09-28', '2026-09-29', '2026-09-30', '2026-10-01', '2026-10-02'] }
    renderWithActions(<MenuCard card={empty} date="2026-09-26" />)

    expect(screen.getByText('menu.nothing_this_week')).toBeDefined()
    expect(screen.queryByRole('button', { name: 'menu.send_week' })).toBeNull()
  })

  it('sends the next seven days to the shopping list', async () => {
    const getShoppingList = vi.fn().mockResolvedValue([{ name: 'Lentilles corail', quantity: 400, unit: 'G' }, { name: 'Potiron', quantity: 1, unit: 'PIECE' }])
    renderWithActions(<MenuCard card={CARD} date="2026-09-26" />, { kitchenApi: { getShoppingList } })

    fireEvent.click(screen.getByRole('button', { name: 'menu.send_week' }))

    expect(await screen.findByText('export-modal 2')).toBeDefined()
    expect(getShoppingList).toHaveBeenCalledWith('space-1', '2026-09-26', '2026-10-02')
  })

  it('says so when the week has nothing to buy', async () => {
    renderWithActions(<MenuCard card={CARD} date="2026-09-26" />, { kitchenApi: { getShoppingList: vi.fn().mockResolvedValue([]) } })

    fireEvent.click(screen.getByRole('button', { name: 'menu.send_week' }))

    expect(await screen.findByText('menu.export_empty')).toBeDefined()
  })

  it('offers neither planning nor export to a viewer', () => {
    renderWithActions(<MenuCard card={CARD} date="2026-09-26" />, { actions: { canWrite: false } })

    expect(screen.queryByRole('link', { name: 'menu.plan' })).toBeNull()
    expect(screen.queryByRole('button', { name: 'menu.send_week' })).toBeNull()
  })
})
