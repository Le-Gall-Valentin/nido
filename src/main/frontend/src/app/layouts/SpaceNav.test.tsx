import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { Users, Calendar } from 'lucide-react'
import { SpaceNav } from './SpaceNav'
import type { SpaceNavItemConfig } from './navConfig'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

const ITEMS: SpaceNavItemConfig[] = [
  { id: 'space:members', to: (id) => `/s/${id}/members`, icon: Users, labelKey: 'nav.members' },
  {
    id: 'space:kitchen',
    to: (id) => `/s/${id}/kitchen/recipes`,
    icon: Calendar,
    labelKey: 'nav.kitchen',
    children: [
      { id: 'space:kitchen:recipes', to: (id) => `/s/${id}/kitchen/recipes`, icon: Calendar, labelKey: 'nav.kitchen_recipes' },
      { id: 'space:kitchen:menu', to: (id) => `/s/${id}/kitchen/menu`, icon: Calendar, labelKey: 'nav.kitchen_menu' },
    ],
  },
]

function renderNav(pathname: string) {
  return render(
    <MemoryRouter>
      <SpaceNav items={ITEMS} spaceId="space-1" pathname={pathname} />
    </MemoryRouter>
  )
}

describe('SpaceNav — back link', () => {
  it('links back to /spaces', () => {
    renderNav('/s/space-1/members')
    const link = screen.getByRole('link', { name: /back_to_groups/ })
    expect(link.getAttribute('href')).toBe('/spaces')
  })
})

describe('SpaceNav — top-level items', () => {
  it('renders every configured item', () => {
    renderNav('/s/space-1/members')
    expect(screen.getByText('nav.members')).toBeDefined()
    expect(screen.getByText('nav.kitchen')).toBeDefined()
  })

  it("links a leaf item straight to its route", () => {
    renderNav('/s/space-1/members')
    const link = screen.getByRole('link', { name: /nav\.members/ })
    expect(link.getAttribute('href')).toBe('/s/space-1/members')
  })
})

describe('SpaceNav — children disclosure', () => {
  it("hides a parent's children when none of them is the active route", () => {
    renderNav('/s/space-1/members')
    expect(screen.queryByText('nav.kitchen_recipes')).toBeNull()
    expect(screen.queryByText('nav.kitchen_menu')).toBeNull()
  })

  it("shows a parent's children when one of them is the active route", () => {
    renderNav('/s/space-1/kitchen/menu')
    expect(screen.getByText('nav.kitchen_recipes')).toBeDefined()
    expect(screen.getByText('nav.kitchen_menu')).toBeDefined()
  })

  it('marks the matching child as active', () => {
    renderNav('/s/space-1/kitchen/menu')
    const menuLink = screen.getByRole('link', { name: /nav\.kitchen_menu/ })
    expect(menuLink.className).toContain('bg-accent-dim')
  })

  it('does not mark a sibling child as active', () => {
    renderNav('/s/space-1/kitchen/menu')
    const recipesLink = screen.getByRole('link', { name: /nav\.kitchen_recipes/ })
    expect(recipesLink.className).not.toContain('bg-accent-dim')
  })
})
