import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import { createTestQueryClient } from '@/shared/test'
import { KitchenRecipeDetailPage } from './KitchenRecipeDetailPage'
import type { IKitchenApi } from '../model/IKitchenApi'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

function fakeApi(): IKitchenApi {
  return {
    listRecipes: vi.fn(),
    getRecipe: vi.fn().mockResolvedValue({
      id: 'r1', name: 'Pâtes bolognaise', category: 'PLAT', minutes: 35, referencePortions: 4, favorite: false,
      ingredients: [{ name: 'Pâtes', quantity: 500, unit: 'GRAM' }],
      steps: ["Faire revenir l'oignon.", 'Ajouter la sauce.'],
    }),
    createRecipe: vi.fn(),
    updateRecipe: vi.fn(),
    deleteRecipe: vi.fn(),
    toggleFavorite: vi.fn(),
    listMenuEntries: vi.fn(),
    addMenuEntry: vi.fn(),
    updateMenuEntryPortions: vi.fn(),
    removeMenuEntry: vi.fn(),
    getShoppingList: vi.fn(),
  }
}

describe('KitchenRecipeDetailPage', () => {
  it('renders the recipe name, ingredients, and steps', async () => {
    const api = fakeApi()
    const queryClient = createTestQueryClient()
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/s/space-1/kitchen/recipes/r1']}>
          <Routes>
            <Route path="/s/:spaceId/kitchen/recipes/:recipeId" element={<KitchenRecipeDetailPage api={api} />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    )

    expect(await screen.findByText('Pâtes bolognaise')).toBeDefined()
    expect(screen.getByText('Pâtes')).toBeDefined()
    expect(screen.getByText('500 unit.GRAM')).toBeDefined()
    expect(screen.getByText("Faire revenir l'oignon.")).toBeDefined()
  })
})
