import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { CommandPalette } from './CommandPalette'
import type { PaletteItem } from '@/shared/lib'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

vi.mock('@/shared/lib', () => ({
  usePaletteResults: vi.fn(),
  useFocusTrap: vi.fn(),
}))

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>()
  return { ...actual, useNavigate: () => mockNavigate }
})

import { usePaletteResults } from '@/shared/lib'
const mockUsePaletteResults = vi.mocked(usePaletteResults)

const Icon = () => null

const ITEMS: PaletteItem[] = [
  { id: 'p1', label: 'Dashboard', to: '/workspace/dashboard', icon: Icon, group: 'Page' },
  { id: 'p2', label: 'Applications', to: '/workspace/applications', icon: Icon, group: 'Page' },
  { id: 'p3', label: 'Exécutions', to: '/workspace/executions', icon: Icon },
]

function setup(items = ITEMS, onClose = vi.fn()) {
  mockUsePaletteResults.mockReturnValue(items)
  render(
    <MemoryRouter>
      <CommandPalette onClose={onClose} />
    </MemoryRouter>
  )
  return { onClose }
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('CommandPalette — rendering', () => {
  it('renders the search input with placeholder', () => {
    setup()
    expect(screen.getByPlaceholderText('palette.placeholder')).toBeDefined()
  })

  it('shows all items when query is empty', () => {
    setup()
    expect(screen.getByText('Dashboard')).toBeDefined()
    expect(screen.getByText('Applications')).toBeDefined()
    expect(screen.getByText('Exécutions')).toBeDefined()
  })

  it('shows group badge when item has a group', () => {
    setup()
    const badges = screen.getAllByText('Page')
    expect(badges.length).toBe(2) // only p1 and p2 have group
  })

  it('shows no results message when query has no match', () => {
    setup()
    fireEvent.change(screen.getByPlaceholderText('palette.placeholder'), {
      target: { value: 'zzz-no-match' },
    })
    expect(screen.getByText('palette.no_results')).toBeDefined()
  })

  it('filters items by query (case-insensitive)', () => {
    setup()
    fireEvent.change(screen.getByPlaceholderText('palette.placeholder'), {
      target: { value: 'dash' },
    })
    expect(screen.getByText('Dashboard')).toBeDefined()
    expect(screen.queryByText('Applications')).toBeNull()
  })
})

describe('CommandPalette — closing', () => {
  it('calls onClose when backdrop is clicked', () => {
    const { onClose } = setup()
    const backdrop = document.querySelector('.fixed.inset-0')!
    fireEvent.click(backdrop)
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('calls onClose when Escape is pressed', () => {
    const { onClose } = setup()
    fireEvent.keyDown(screen.getByPlaceholderText('palette.placeholder'), {
      key: 'Escape',
    })
    expect(onClose).toHaveBeenCalledOnce()
  })
})

describe('CommandPalette — navigation', () => {
  it('navigates to item.to and calls onClose when item is clicked', () => {
    const { onClose } = setup()
    fireEvent.click(screen.getByText('Applications'))
    expect(mockNavigate).toHaveBeenCalledWith('/workspace/applications')
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('navigates to the selected item on Enter', () => {
    const { onClose } = setup()
    const input = screen.getByPlaceholderText('palette.placeholder')
    fireEvent.keyDown(input, { key: 'Enter' })
    // first item is selected by default
    expect(mockNavigate).toHaveBeenCalledWith('/workspace/dashboard')
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('moves selection down with ArrowDown and navigates on Enter', () => {
    const { onClose } = setup()
    const input = screen.getByPlaceholderText('palette.placeholder')
    fireEvent.keyDown(input, { key: 'ArrowDown' })
    fireEvent.keyDown(input, { key: 'Enter' })
    expect(mockNavigate).toHaveBeenCalledWith('/workspace/applications')
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('ArrowUp does not go below index 0', () => {
    const { onClose } = setup()
    const input = screen.getByPlaceholderText('palette.placeholder')
    fireEvent.keyDown(input, { key: 'ArrowUp' }) // already at 0
    fireEvent.keyDown(input, { key: 'Enter' })
    expect(mockNavigate).toHaveBeenCalledWith('/workspace/dashboard')
    expect(onClose).toHaveBeenCalledOnce()
  })
})