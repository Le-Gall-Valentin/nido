import { render, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { LeaveSpaceModal } from './LeaveSpaceModal'
import { LastOwnerError } from '../api/spacesPageApi'

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

function setup(overrides: { onLeave?: () => Promise<void> } = {}) {
  const onClose = vi.fn()
  const onSuccess = vi.fn()
  const onLeave = overrides.onLeave ?? vi.fn().mockResolvedValue(undefined)
  const result = render(<LeaveSpaceModal spaceName="Chez nous" onClose={onClose} onLeave={onLeave} onSuccess={onSuccess} />)
  return { ...result, onClose, onSuccess, onLeave }
}

describe('LeaveSpaceModal', () => {
  it('calls onLeave and onSuccess when confirmed', async () => {
    const { getByText, onLeave, onSuccess } = setup()
    fireEvent.click(getByText('leave.submit'))
    await waitFor(() => expect(onSuccess).toHaveBeenCalledOnce())
    expect(onLeave).toHaveBeenCalledOnce()
  })

  it('calls onClose when cancelled', () => {
    const { getByText, onClose } = setup()
    fireEvent.click(getByText('leave.cancel'))
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('shows the last-owner error and does not call onSuccess', async () => {
    const { getByText, onSuccess, findByRole } = setup({ onLeave: vi.fn().mockRejectedValue(new LastOwnerError()) })
    fireEvent.click(getByText('leave.submit'))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('leave.error.last_owner')
    expect(onSuccess).not.toHaveBeenCalled()
  })

  it('prevents a double submit', async () => {
    let resolve!: () => void
    const onLeave = vi.fn().mockImplementation(() => new Promise<void>((r) => { resolve = r }))
    const { getByText } = setup({ onLeave })
    fireEvent.click(getByText('leave.submit'))
    fireEvent.click(getByText('leave.submit'))
    resolve()
    await waitFor(() => expect(onLeave).toHaveBeenCalledOnce())
  })
})
