import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { Users, Calendar } from 'lucide-react'
import { NavList } from './NavList'
import type { NavItemConfig } from './navConfig'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

const ITEMS: NavItemConfig[] = [
  { id: 'nav:spaces', to: () => '/spaces', icon: Users, labelKey: 'nav.groups' },
  { id: 'nav:members', to: (spaceId) => (spaceId ? `/s/${spaceId}/members` : undefined), icon: Users, labelKey: 'nav.members' },
  {
    id: 'nav:kitchen',
    to: (spaceId) => (spaceId ? `/s/${spaceId}/kitchen/recipes` : undefined),
    icon: Calendar,
    labelKey: 'nav.kitchen',
    children: [
      { id: 'nav:kitchen:recipes', to: (spaceId) => (spaceId ? `/s/${spaceId}/kitchen/recipes` : undefined), icon: Calendar, labelKey: 'nav.kitchen_recipes' },
      { id: 'nav:kitchen:menu', to: (spaceId) => (spaceId ? `/s/${spaceId}/kitchen/menu` : undefined), icon: Calendar, labelKey: 'nav.kitchen_menu' },
    ],
  },
]

function renderList(spaceId: string | undefined, pathname: string, alwaysExpanded = false, hasPendingInvitations = false) {
  return render(
    <MemoryRouter>
      <NavList
        items={ITEMS}
        spaceId={spaceId}
        pathname={pathname}
        alwaysExpanded={alwaysExpanded}
        hasPendingInvitations={hasPendingInvitations}
      />
    </MemoryRouter>
  )
}

describe('NavList — items that need no space', () => {
  it('always renders regardless of the resolved space', () => {
    renderList(undefined, '/spaces')
    expect(screen.getByText('nav.groups')).toBeDefined()
  })
})

describe('NavList — items that need a space', () => {
  it('is skipped (not rendered as a dead link) while the space is unresolved', () => {
    renderList(undefined, '/spaces')
    expect(screen.queryByText('nav.members')).toBeNull()
    expect(screen.queryByText('nav.kitchen')).toBeNull()
  })

  it('links to the resolved space once known', () => {
    renderList('space-1', '/s/space-1/members')
    const link = screen.getByRole('link', { name: /nav\.members/ })
    expect(link.getAttribute('href')).toBe('/s/space-1/members')
  })
})

describe('NavList — children disclosure', () => {
  it("hides a parent's children when none of them is the active route", () => {
    renderList('space-1', '/s/space-1/members')
    expect(screen.queryByText('nav.kitchen_recipes')).toBeNull()
    expect(screen.queryByText('nav.kitchen_menu')).toBeNull()
  })

  it("shows a parent's children when one of them is the active route", () => {
    renderList('space-1', '/s/space-1/kitchen/menu')
    expect(screen.getByText('nav.kitchen_recipes')).toBeDefined()
    expect(screen.getByText('nav.kitchen_menu')).toBeDefined()
  })

  it('marks the matching child as active', () => {
    renderList('space-1', '/s/space-1/kitchen/menu')
    const menuLink = screen.getByRole('link', { name: /nav\.kitchen_menu/ })
    expect(menuLink.className).toContain('bg-accent-dim')
  })

  it('keeps the parent highlighted while a sibling child is active, not just its own link', () => {
    renderList('space-1', '/s/space-1/kitchen/menu')
    const parentLink = screen.getByRole('link', { name: /^nav\.kitchen$/ })
    expect(parentLink.className).toContain('bg-accent-dim')
  })

  it('expands every parent with children when alwaysExpanded, regardless of the active route', () => {
    renderList('space-1', '/s/space-1/members', true)
    expect(screen.getByText('nav.kitchen_recipes')).toBeDefined()
    expect(screen.getByText('nav.kitchen_menu')).toBeDefined()
  })
})

describe('NavList — pending invitations badge', () => {
  it('shows a badge on the groups item when hasPendingInvitations is true', () => {
    renderList(undefined, '/spaces', false, true)
    expect(screen.getByText('nav.pending_invitations')).toBeDefined()
  })

  it('shows no badge when hasPendingInvitations is false', () => {
    renderList(undefined, '/spaces', false, false)
    expect(screen.queryByText('nav.pending_invitations')).toBeNull()
  })

  it('does not badge unrelated items even when hasPendingInvitations is true', () => {
    renderList('space-1', '/s/space-1/kitchen/menu', true, true)
    // Only one badge total: the groups item, never kitchen or its children.
    expect(screen.getAllByText('nav.pending_invitations')).toHaveLength(1)
  })
})
