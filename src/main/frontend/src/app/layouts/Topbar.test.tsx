import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { Topbar } from './Topbar'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

vi.mock('./useBreadcrumbs', () => ({
  useBreadcrumbs: vi.fn(),
}))

import { useBreadcrumbs } from './useBreadcrumbs'
const mockUseBreadcrumbs = vi.mocked(useBreadcrumbs)

function setup(
  crumbs = [{ label: 'Espace de travail', to: '/workspace' }, { label: 'Tableau de bord' }],
  onMenuOpen = vi.fn(),
  onSearchOpen = vi.fn()
) {
  mockUseBreadcrumbs.mockReturnValue(crumbs)
  render(
    <MemoryRouter>
      <Topbar onMenuOpen={onMenuOpen} onSearchOpen={onSearchOpen} />
    </MemoryRouter>
  )
  return { onMenuOpen, onSearchOpen }
}

beforeEach(() => vi.clearAllMocks())

describe('Topbar — hamburger', () => {
  it('calls onMenuOpen when the hamburger button is clicked', () => {
    const { onMenuOpen } = setup()
    fireEvent.click(screen.getByLabelText('topbar.menu_label'))
    expect(onMenuOpen).toHaveBeenCalledOnce()
  })
})

describe('Topbar — search', () => {
  it('calls onSearchOpen when the search trigger is clicked', () => {
    const { onSearchOpen } = setup()
    // There are two search buttons (sm+ and mobile), click the first visible one
    const searchButtons = screen.getAllByLabelText('topbar.search_label')
    fireEvent.click(searchButtons[0])
    expect(onSearchOpen).toHaveBeenCalledOnce()
  })
})

describe('Topbar — breadcrumbs', () => {
  it('renders all breadcrumb labels', () => {
    setup([
      { label: 'Espace de travail', to: '/workspace' },
      { label: 'Applications', to: '/workspace/applications' },
      { label: 'my-app' },
    ])
    expect(screen.getByText('Espace de travail')).toBeDefined()
    expect(screen.getByText('Applications')).toBeDefined()
    expect(screen.getByText('my-app')).toBeDefined()
  })

  it('renders intermediate labels as links', () => {
    setup([
      { label: 'Espace de travail', to: '/workspace' },
      { label: 'Applications', to: '/workspace/applications' },
      { label: 'my-app' },
    ])
    const link = screen.getByRole('link', { name: 'Espace de travail' })
    expect(link.getAttribute('href')).toBe('/workspace')
  })

  it('renders the last breadcrumb as plain text (not a link)', () => {
    setup([{ label: 'Espace de travail', to: '/workspace' }, { label: 'Dashboard' }])
    // 'Dashboard' should not be a link
    expect(screen.queryByRole('link', { name: 'Dashboard' })).toBeNull()
    expect(screen.getByText('Dashboard')).toBeDefined()
  })

  it('renders separator / between breadcrumbs', () => {
    setup([{ label: 'A', to: '/a' }, { label: 'B' }])
    expect(screen.getByText('/')).toBeDefined()
  })
})

describe('Topbar — refresh', () => {
  it('renders the refresh button', () => {
    setup()
    expect(screen.getByLabelText('topbar.refresh_label')).toBeDefined()
  })
})