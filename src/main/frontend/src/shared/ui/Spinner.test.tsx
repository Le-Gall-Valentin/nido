import { render } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { Spinner } from './Spinner'

describe('Spinner', () => {
  it('renders with default label', () => {
    const { container } = render(<Spinner />)
    expect(container.querySelector('[role="status"]')).not.toBeNull()
  })

  it('renders with custom label', () => {
    const { container } = render(<Spinner label="Chargement..." />)
    const el = container.querySelector('[role="status"]')
    expect(el?.getAttribute('aria-label')).toBe('Chargement...')
  })

  it('includes min-h-screen class when fullscreen is true (default)', () => {
    const { container } = render(<Spinner />)
    expect(container.querySelector('[role="status"]')?.className).toContain('min-h-screen')
  })

  it('omits min-h-screen class when fullscreen is false', () => {
    const { container } = render(<Spinner fullscreen={false} />)
    expect(container.querySelector('[role="status"]')?.className).not.toContain('min-h-screen')
  })
})