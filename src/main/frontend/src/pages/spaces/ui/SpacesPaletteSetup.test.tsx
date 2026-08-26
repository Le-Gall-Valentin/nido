import { render } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { SpacesPaletteSetup } from './SpacesPaletteSetup'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts?.name ? `${k}:${opts.name}` : k) }),
}))

const mockUsePaletteItems = vi.fn()
vi.mock('@/shared/lib', () => ({
  usePaletteItems: (...args: unknown[]) => mockUsePaletteItems(...args),
}))

vi.mock('@/shared/config', () => ({
  ROUTES: { SPACES: '/spaces', spaceMembers: (id: string) => `/s/${id}/members` },
}))

const mockUseMySpaces = vi.fn()
vi.mock('@/features/space-switcher', () => ({
  useMySpaces: () => mockUseMySpaces(),
}))

beforeEach(() => vi.clearAllMocks())

describe('SpacesPaletteSetup', () => {
  it('renders nothing', () => {
    mockUseMySpaces.mockReturnValue({ data: [] })
    const { container } = render(<SpacesPaletteSetup />)
    expect(container.firstChild).toBeNull()
  })

  it('registers the page command plus one switch command per context', () => {
    mockUseMySpaces.mockReturnValue({
      data: [
        { id: 's-1', type: 'SHARED', name: 'Chez nous', accent: '#c17a5c', glyph: '🏡', myRole: 'OWNER', memberCount: 1 },
        { id: 'p-1', type: 'PERSONAL', name: 'Alice', accent: '#8a7d6b', glyph: '👤', myRole: 'OWNER', memberCount: 1 },
      ],
    })
    render(<SpacesPaletteSetup />)
    expect(mockUsePaletteItems).toHaveBeenCalledWith('spaces', expect.any(Array))
    const items = mockUsePaletteItems.mock.calls[0][1] as { id: string; to: string }[]
    expect(items.map((i) => i.id)).toEqual(['spaces:page', 'spaces:switch:s-1', 'spaces:switch:p-1'])
    expect(items[1].to).toBe('/s/s-1/members')
  })

  it('registers only the page command when there are no contexts yet', () => {
    mockUseMySpaces.mockReturnValue({ data: undefined })
    render(<SpacesPaletteSetup />)
    const items = mockUsePaletteItems.mock.calls[0][1] as { id: string }[]
    expect(items.map((i) => i.id)).toEqual(['spaces:page'])
  })
})
