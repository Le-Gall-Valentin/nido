import { render } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { Button } from './Button'

describe('Button', () => {
  it('renders children when not loading', () => {
    const { getByText } = render(<Button>Submit</Button>)
    expect(getByText('Submit')).toBeDefined()
  })

  it('hides spinner from screen readers when loading', () => {
    const { container } = render(<Button isLoading>Submit</Button>)
    const spinner = container.querySelector('svg')
    expect(spinner?.getAttribute('aria-hidden')).toBe('true')
  })

  it('preserves accessible label for screen readers when loading', () => {
    const { container } = render(<Button isLoading>Submit</Button>)
    const srSpan = container.querySelector('.sr-only')
    expect(srSpan?.textContent).toBe('Submit')
  })

  it('is disabled when isLoading is true', () => {
    const { container } = render(<Button isLoading>Submit</Button>)
    expect((container.querySelector('button') as HTMLButtonElement).disabled).toBe(true)
  })
})