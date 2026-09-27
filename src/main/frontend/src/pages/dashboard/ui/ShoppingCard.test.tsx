import { screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { ShoppingCard } from './ShoppingCard'
import { renderWithActions } from '../test/renderWithActions'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

describe('ShoppingCard', () => {
  it('counts what is left to buy and previews each aisle in list order', () => {
    renderWithActions(<ShoppingCard card={{ remaining: 12, categories: [
      { categoryId: 'c-veg', name: 'Fruits et légumes', count: 5, preview: ['Tomates', 'Courgettes', 'Pommes'] },
      { categoryId: 'c-dairy', name: 'Frais', count: 3, preview: ['Lait', 'Beurre', 'Crème fraîche'] },
    ] }} />)

    expect(screen.getByText('shopping.count:{"count":12}')).toBeDefined()
    expect(screen.getByText('Tomates, Courgettes, Pommes shopping.and_more:{"count":2}')).toBeDefined()
    expect(screen.getByText('Lait, Beurre, Crème fraîche')).toBeDefined()
    expect(screen.getByRole('link', { name: 'shopping.link' }).getAttribute('href')).toBe('/s/space-1/organisation/courses')
  })
})
