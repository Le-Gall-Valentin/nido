import { render, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { Alert } from './Alert'

describe('Alert', () => {
  it('renders error variant with role="alert"', () => {
    const { getByRole } = render(<Alert variant="error">Boom</Alert>)
    expect(getByRole('alert').textContent).toContain('Boom')
  })

  it('renders warning variant with role="status"', () => {
    const { getByRole } = render(<Alert variant="warning">Careful</Alert>)
    expect(getByRole('status').textContent).toContain('Careful')
  })

  it('renders success variant with role="status"', () => {
    const { getByRole } = render(<Alert variant="success">Done</Alert>)
    expect(getByRole('status').textContent).toContain('Done')
  })

  it('renders no dismiss button by default', () => {
    const { queryByRole } = render(<Alert variant="error">Boom</Alert>)
    expect(queryByRole('button')).toBeNull()
  })

  it('calls onDismiss when dismiss button is clicked', () => {
    const onDismiss = vi.fn()
    const { getByRole } = render(
      <Alert variant="error" onDismiss={onDismiss} dismissLabel="Close">Boom</Alert>
    )
    fireEvent.click(getByRole('button', { name: 'Close' }))
    expect(onDismiss).toHaveBeenCalledOnce()
  })
})
