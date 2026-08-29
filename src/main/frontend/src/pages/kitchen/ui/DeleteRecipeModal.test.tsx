import { render, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { DeleteRecipeModal } from './DeleteRecipeModal'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

vi.mock('@/shared/ui', () => ({
  Alert: ({ children, variant }: { children: React.ReactNode; variant: string }) => (
    <div role={variant === 'error' ? 'alert' : 'status'}>{children}</div>
  ),
  Dialog: ({ children, open }: { children: React.ReactNode; open: boolean }) =>
    open ? <div data-testid="dialog">{children}</div> : null,
  Button: ({ children, onClick, disabled, isLoading, type, ...props }: React.ButtonHTMLAttributes<HTMLButtonElement> & { isLoading?: boolean; children: React.ReactNode }) => (
    <button type={type} onClick={onClick} disabled={disabled || isLoading} {...props}>{children}</button>
  ),
}))

beforeEach(() => { vi.clearAllMocks() })

function setup(overrides: { onDelete?: () => Promise<void> } = {}) {
  const onClose = vi.fn()
  const onDelete = overrides.onDelete ?? vi.fn().mockResolvedValue(undefined)
  const result = render(<DeleteRecipeModal recipeName="Pâtes bolognaise" onClose={onClose} onDelete={onDelete} />)
  return { ...result, onClose, onDelete }
}

describe('DeleteRecipeModal', () => {
  it('calls onDelete and then onClose when confirmed', async () => {
    const { getByText, onDelete, onClose } = setup()
    fireEvent.click(getByText('delete_confirm.submit'))
    await waitFor(() => expect(onClose).toHaveBeenCalledOnce())
    expect(onDelete).toHaveBeenCalledOnce()
  })

  it('calls onClose without calling onDelete when cancelled', () => {
    const { getByText, onClose, onDelete } = setup()
    fireEvent.click(getByText('delete_confirm.cancel'))
    expect(onClose).toHaveBeenCalledOnce()
    expect(onDelete).not.toHaveBeenCalled()
  })

  it('shows an error and does not call onClose when deletion fails', async () => {
    const { getByText, onClose, findByRole } = setup({ onDelete: vi.fn().mockRejectedValue(new Error('boom')) })
    fireEvent.click(getByText('delete_confirm.submit'))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('delete_confirm.error')
    expect(onClose).not.toHaveBeenCalled()
  })
})
