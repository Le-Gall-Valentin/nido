import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { LoginBrandPanel } from './LoginBrandPanel'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

describe('LoginBrandPanel', () => {
  it('renders the brand name and the NidoMark', () => {
    const { container } = render(<LoginBrandPanel />)
    expect(screen.getByText('brand')).not.toBeNull()
    expect(container.querySelector('svg')).not.toBeNull()
  })

  it('renders the hero copy', () => {
    render(<LoginBrandPanel />)
    expect(screen.getByText(/hero\.headline_1/)).not.toBeNull()
    expect(screen.getByText('hero.description')).not.toBeNull()
  })

  it('does not render the quote block', () => {
    render(<LoginBrandPanel />)
    expect(screen.queryByText('quote')).toBeNull()
  })
})
