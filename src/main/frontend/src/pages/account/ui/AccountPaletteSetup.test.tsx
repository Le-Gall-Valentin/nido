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
  ROUTES: { ACCOUNT: '/account' },
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

  it('registers 5 items including page and 4 sections', () => {
    render(<AccountPaletteSetup />)
    const items = mockUsePaletteItems.mock.calls[0][1] as { id: string }[]
    expect(items).toHaveLength(5)
    expect(items.map(i => i.id)).toEqual([
      'account:page',
      'account:profile',
      'account:twofa',
      'account:preferences',
      'account:password',
    ])
  })
})