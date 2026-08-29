import { render } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { AccountPaletteSetup } from './AccountPaletteSetup'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

const mockUsePaletteItems = vi.fn()
vi.mock('@/shared/lib', () => ({
  usePaletteItems: (...args: unknown[]) => mockUsePaletteItems(...args),
}))

vi.mock('@/shared/config', () => ({
  ROUTES: { ACCOUNT_PROFILE: '/account/profile', ACCOUNT_SECURITY: '/account/security', ACCOUNT_PREFERENCES: '/account/preferences' },
}))

describe('AccountPaletteSetup', () => {
  it('renders nothing', () => {
    const { container } = render(<AccountPaletteSetup />)
    expect(container.firstChild).toBeNull()
  })

  it('registers palette items under the account namespace', () => {
    render(<AccountPaletteSetup />)
    expect(mockUsePaletteItems).toHaveBeenCalledWith('account', expect.any(Array))
  })

  it('registers the 3 real sub-pages, one per navigable route', () => {
    render(<AccountPaletteSetup />)
    const items = mockUsePaletteItems.mock.calls[0][1] as { id: string; to: string }[]
    expect(items.map(i => i.id)).toEqual([
      'account:profile',
      'account:security',
      'account:preferences',
    ])
    expect(items.map(i => i.to)).toEqual([
      '/account/profile',
      '/account/security',
      '/account/preferences',
    ])
  })
})