import { describe, it, expect, vi } from 'vitest'
import { fireEvent, screen, waitFor } from '@testing-library/react'
import { KitchenApiProvider, type IKitchenApi, type MenuEntry, type Recipe } from '@/entities/kitchen'
import { renderWithQuery } from '@/shared/test'
import { MealEntryModal } from './MealEntryModal'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string, options?: { name?: string }) => (options?.name ? `${key}:${options.name}` : key) }),
}))

const recipe = (id: string, name: string): Recipe => ({
  id, name, category: 'PLAT', minutes: 30, referencePortions: 4, favorite: false, ingredients: [], steps: [],
})
const gratin = recipe('r1', 'Gratin')
const soup = recipe('r2', 'Soupe')
const planned: MenuEntry = {
  id: 'm1', date: '2026-10-06', recipeId: 'r2', recipeName: 'Soupe', recipeCategory: 'SOUP', portions: 2, position: 0,
}

function renderModal({ entries = [] as MenuEntry[], addFails = false, recipes = Promise.resolve([gratin, soup]) } = {}) {
  const api = {
    listRecipes: vi.fn(() => recipes),
    listMenuEntries: vi.fn().mockResolvedValue(entries),
    addMenuEntry: vi.fn(() => (addFails ? Promise.reject(new Error('down')) : Promise.resolve(planned))),
    removeMenuEntry: vi.fn().mockResolvedValue(undefined),
  }
  renderWithQuery(
    <KitchenApiProvider api={api as unknown as IKitchenApi}>
      <MealEntryModal spaceId="space-1" date="2026-10-06" onClose={vi.fn()} />
    </KitchenApiProvider>)
  return api
}

async function pick(recipeId: string) {
  await screen.findByRole('option', { name: 'Gratin' })
  fireEvent.change(screen.getByRole('combobox'), { target: { value: recipeId } })
}

function setPortions(value: string) {
  fireEvent.change(screen.getByLabelText('meal.portions'), { target: { value } })
}

describe('MealEntryModal', () => {
  it('waits for the recipes before offering them', () => {
    renderModal({ recipes: new Promise(() => {}) })
    expect(screen.getByRole('status')).toBeTruthy()
    expect(screen.queryByRole('combobox')).toBeNull()
  })

  it('plans the recipe picked for the day, for as many as asked', async () => {
    const api = renderModal()
    await pick('r1')
    setPortions('6')
    fireEvent.click(screen.getByRole('button', { name: 'meal.add' }))
    await waitFor(() => expect(api.addMenuEntry).toHaveBeenCalledWith('space-1', '2026-10-06', 'r1', 6))
    // Ready for the next dish of the same day.
    await waitFor(() => expect((screen.getByRole('combobox') as HTMLSelectElement).value).toBe(''))
  })

  it('asks for a recipe before planning anything', async () => {
    const api = renderModal()
    await screen.findByRole('combobox')
    fireEvent.click(screen.getByRole('button', { name: 'meal.add' }))
    expect(screen.getByText('meal.recipe_required')).toBeTruthy()
    expect(api.addMenuEntry).not.toHaveBeenCalled()
  })

  it.each(['0', '1.5', ''])('refuses %j portions', async (portions) => {
    const api = renderModal()
    await pick('r1')
    setPortions(portions)
    fireEvent.click(screen.getByRole('button', { name: 'meal.add' }))
    expect(screen.getByText('meal.portions_invalid')).toBeTruthy()
    expect(api.addMenuEntry).not.toHaveBeenCalled()
  })

  it('says so when the meal could not be planned', async () => {
    renderModal({ addFails: true })
    await pick('r1')
    fireEvent.click(screen.getByRole('button', { name: 'meal.add' }))
    expect(await screen.findByText('meal.save_failed')).toBeTruthy()
  })

  it('shows what the day already has, and takes a meal off it', async () => {
    const api = renderModal({ entries: [planned] })
    expect(api.listMenuEntries).toHaveBeenCalledWith('space-1', '2026-10-06', '2026-10-06')
    fireEvent.click(await screen.findByRole('button', { name: 'meal.remove:Soupe' }))
    await waitFor(() => expect(api.removeMenuEntry).toHaveBeenCalledWith('space-1', 'm1'))
  })
})
