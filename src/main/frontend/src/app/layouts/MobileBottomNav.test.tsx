import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { Settings, Users } from 'lucide-react'
import { MobileBottomNav } from './MobileBottomNav'
import type { NavItemConfig } from './navConfig'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

const ITEMS: NavItemConfig[] = [
  { id: 'nav:spaces', to: () => '/spaces', icon: Users, labelKey: 'nav.groups' },
  {
    id: 'nav:account', to: () => '/account/profile', icon: Settings, labelKey: 'nav.settings',
    children: [
      { id: 'nav:account:profile', to: () => '/account/profile', icon: Settings, labelKey: 'nav.settings_profile' },
      { id: 'nav:account:security', to: () => '/account/security', icon: Settings, labelKey: 'nav.settings_security' },
    ],
  },
]

function renderNav(pathname: string) {
  return render(
    <MemoryRouter>
      <MobileBottomNav items={ITEMS} spaceId={undefined} pathname={pathname} />
    </MemoryRouter>
  )
}

describe('MobileBottomNav', () => {
  it('renders every resolved item as a tab', () => {
    renderNav('/spaces')
    expect(screen.getByText('nav.groups')).toBeDefined()
    expect(screen.getByText('nav.settings')).toBeDefined()
  })

  it("skips an item whose destination hasn't resolved", () => {
    const items: NavItemConfig[] = [
      { id: 'nav:members', to: () => undefined, icon: Users, labelKey: 'nav.members' },
    ]
    render(
      <MemoryRouter>
        <MobileBottomNav items={items} spaceId={undefined} pathname="/spaces" />
      </MemoryRouter>
    )
    expect(screen.queryByText('nav.members')).toBeNull()
  })

  it('marks a leaf tab active on an exact match', () => {
    renderNav('/spaces')
    const link = screen.getByRole('link', { name: /nav\.groups/ })
    expect(link.className).toContain('text-status-green')
  })

  it('marks the parent tab active while on any of its children', () => {
    renderNav('/account/security')
    const link = screen.getByRole('link', { name: /nav\.settings/ })
    expect(link.className).toContain('text-status-green')
  })

  it('does not mark an unrelated tab active', () => {
    renderNav('/spaces')
    const link = screen.getByRole('link', { name: /nav\.settings/ })
    expect(link.className).not.toContain('text-status-green')
  })
})
