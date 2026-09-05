import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { DeleteTransactionModal } from './DeleteTransactionModal'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

describe('DeleteTransactionModal', () => {
  it('confirms deletion of the named transaction', () => {
    const onConfirm = vi.fn()
    render(<DeleteTransactionModal label="Courses" onConfirm={onConfirm} onCancel={vi.fn()} isPending={false} error={null} />)

    expect(screen.getByText(/Courses/)).toBeDefined()
    fireEvent.click(screen.getByText('delete_confirm.confirm'))

    expect(onConfirm).toHaveBeenCalled()
  })

  it('shows an error message when provided', () => {
    render(<DeleteTransactionModal label="Courses" onConfirm={vi.fn()} onCancel={vi.fn()} isPending={false} error="boom" />)

    expect(screen.getByText('delete_confirm.error')).toBeDefined()
  })
})
