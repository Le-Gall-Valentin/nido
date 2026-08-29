import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { Users, Calendar } from 'lucide-react'
import { MobileNavDrawer } from './MobileNavDrawer'
import type { NavItemConfig } from './navConfig'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

const ITEMS: NavItemConfig[] = [
  { id: 'nav:spaces', to: () => '/spaces', icon: Users, labelKey: 'nav.groups' },
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

function setup(onClose = vi.fn()) {
  render(
    <MemoryRouter>
      <MobileNavDrawer items={ITEMS} spaceId="space-1" pathname="/spaces" onClose={onClose} />
    </MemoryRouter>
  )
  return { onClose }
}

describe('MobileNavDrawer', () => {
  it('shows every sub-item expanded at all times, regardless of the active route', () => {
    setup()
    expect(screen.getByText('nav.kitchen_recipes')).toBeDefined()
    expect(screen.getByText('nav.kitchen_menu')).toBeDefined()
  })

  it('closes when the backdrop is clicked', () => {
    const { onClose } = setup()
    fireEvent.click(screen.getAllByLabelText('topbar.close_menu_label')[0])
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('closes when the close button is clicked', () => {
    const { onClose } = setup()
    fireEvent.click(screen.getAllByLabelText('topbar.close_menu_label')[1])
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('closes on Escape', () => {
    const { onClose } = setup()
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('closes when a nav item is clicked', () => {
    const { onClose } = setup()
    fireEvent.click(screen.getByText('nav.groups'))
    expect(onClose).toHaveBeenCalledOnce()
  })
})
