import { render, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { Dialog } from './Dialog'

vi.mock('@/shared/lib', () => ({
  useFocusTrap: vi.fn(),
}))

describe('Dialog', () => {
  it('renders nothing when open is false', () => {
    const { container } = render(
      <Dialog open={false} onClose={vi.fn()} title="Test">
        <p>Content</p>
      </Dialog>
    )
    expect(container.firstChild).toBeNull()
  })

  it('renders children when open is true', () => {
    const { getByText } = render(
      <Dialog open={true} onClose={vi.fn()} title="Test">
        <p>Content</p>
      </Dialog>
    )
    expect(getByText('Content')).toBeDefined()
  })

  it('calls onClose when Escape is pressed', () => {
    const onClose = vi.fn()
    render(
      <Dialog open={true} onClose={onClose} title="Test">
        <p>Content</p>
      </Dialog>
    )
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(onClose).toHaveBeenCalledTimes(1)
  })

  it('calls onClose when backdrop is clicked', () => {
    const onClose = vi.fn()
    const { getByTestId } = render(
      <Dialog open={true} onClose={onClose} title="Test">
        <p>Content</p>
      </Dialog>
    )
    fireEvent.click(getByTestId('backdrop'))
    expect(onClose).toHaveBeenCalledTimes(1)
  })

  it('has role=dialog and aria-modal on the container', () => {
    const { container } = render(
      <Dialog open={true} onClose={vi.fn()} title="Accessible">
        <p>Content</p>
      </Dialog>
    )
    const dialog = container.querySelector('[role="dialog"]')
    expect(dialog).toBeDefined()
    expect(dialog?.getAttribute('aria-modal')).toBe('true')
  })

  it('links aria-labelledby to the sr-only title element', () => {
    const { container } = render(
      <Dialog open={true} onClose={vi.fn()} title="My Dialog Title">
        <p>Content</p>
      </Dialog>
    )
    const dialog = container.querySelector('[role="dialog"]')
    const titleId = dialog?.getAttribute('aria-labelledby')
    expect(titleId).toBeTruthy()
    const h2 = container.querySelector('h2')
    expect(h2?.id).toBe(titleId)
    expect(h2?.textContent).toBe('My Dialog Title')
  })
})