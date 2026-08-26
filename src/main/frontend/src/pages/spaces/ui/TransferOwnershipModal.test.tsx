import { render, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { TransferOwnershipModal } from './TransferOwnershipModal'
import { OwnerProtectedError } from '../api/spacesPageApi'
import type { SpaceMember } from '@/entities/space'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
}))

vi.mock('@/shared/ui', () => ({
  Alert: ({ children, variant }: { children: React.ReactNode; variant: string }) => (
    <div role={variant === 'error' ? 'alert' : variant === 'warning' ? 'status' : 'status'}>{children}</div>
  ),
  Dialog: ({ children, open }: { children: React.ReactNode; open: boolean }) =>
    open ? <div data-testid="dialog">{children}</div> : null,
  Button: ({ children, onClick, disabled, isLoading, type, ...props }: React.ButtonHTMLAttributes<HTMLButtonElement> & { isLoading?: boolean; children: React.ReactNode }) => (
    <button type={type} onClick={onClick} disabled={disabled || isLoading} {...props}>{children}</button>
  ),
}))

const TARGET: SpaceMember = { userId: 'u-2', username: 'bob', email: 'bob@test.com', role: 'ADMIN', joinedAt: '2024-01-01T00:00:00Z' }
const DELETED_TARGET: SpaceMember = { userId: 'u-3', username: null, email: null, role: 'MEMBER', joinedAt: '2024-01-01T00:00:00Z' }

beforeEach(() => { vi.clearAllMocks() })

function setup(overrides: { onTransfer?: () => Promise<void>; target?: SpaceMember } = {}) {
  const onClose = vi.fn()
  const onSuccess = vi.fn()
  const onTransfer = overrides.onTransfer ?? vi.fn().mockResolvedValue(undefined)
  const result = render(
    <TransferOwnershipModal target={overrides.target ?? TARGET} onClose={onClose} onTransfer={onTransfer} onSuccess={onSuccess} />
  )
  return { ...result, onClose, onSuccess, onTransfer }
}

describe('TransferOwnershipModal', () => {
  it('calls onTransfer with the target user id and onSuccess', async () => {
    const { getByText, onTransfer, onSuccess } = setup()
    fireEvent.click(getByText('transfer.submit'))
    await waitFor(() => expect(onSuccess).toHaveBeenCalledOnce())
    expect(onTransfer).toHaveBeenCalledWith('u-2')
  })

  it('calls onClose when cancelled', () => {
    const { getByText, onClose } = setup()
    fireEvent.click(getByText('transfer.cancel'))
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('shows an error and does not call onSuccess', async () => {
    const { getByText, onSuccess, findByRole } = setup({ onTransfer: vi.fn().mockRejectedValue(new OwnerProtectedError()) })
    fireEvent.click(getByText('transfer.submit'))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('transfer.error.owner_protected')
    expect(onSuccess).not.toHaveBeenCalled()
  })

  it('renders a deleted-account target without crashing', () => {
    expect(() => setup({ target: DELETED_TARGET })).not.toThrow()
  })
})
