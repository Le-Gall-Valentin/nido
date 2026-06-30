import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { LoginBrandPanel } from './LoginBrandPanel'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

describe('LoginBrandPanel', () => {
  it('renders the "B" monogram', () => {
    render(<LoginBrandPanel />)
    expect(screen.getByText('B')).not.toBeNull()
  })

  it('does not render the network artwork svg', () => {
    const { container } = render(<LoginBrandPanel />)
    expect(container.querySelector('svg')).toBeNull()
  })

  it('does not render the quote block', () => {
    render(<LoginBrandPanel />)
    expect(screen.queryByText('quote')).toBeNull()
  })
})