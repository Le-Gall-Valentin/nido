import { render, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { DeleteUserModal } from './DeleteUserModal'
import { NetworkError, ServerError } from '@/shared/lib'
import type { AdminUser } from '../api/adminUsersApi'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, string>) => {
    if (opts?.username) return `${k}:${opts.username}`
    return k
  }}),
}))

vi.mock('@/shared/ui', () => ({
  Alert: ({ children, variant }: { children: React.ReactNode; variant: string }) => (
    <div role={variant === 'error' ? 'alert' : 'status'}>{children}</div>
  ),
  Dialog: ({ children, open }: { children: React.ReactNode; open: boolean }) =>
    open ? <div data-testid="dialog">{children}</div> : null,
  Button: ({ children, onClick, disabled, isLoading, ...props }: React.ButtonHTMLAttributes<HTMLButtonElement> & { isLoading?: boolean; children: React.ReactNode }) => (
    <button onClick={onClick} disabled={disabled || isLoading} {...props}>{children}</button>
  ),
}))

const TARGET: AdminUser = {
  id: 'u-1', username: 'alice', email: 'alice@test.com',
  role: 'USER', isActive: true, createdAt: '2024-01-01T00:00:00Z', totpEnabled: false,
}

function setup(overrides: { onDelete?: () => Promise<void> } = {}) {
  const onClose = vi.fn()
  const onSuccess = vi.fn()
  const onDelete = overrides.onDelete ?? vi.fn().mockResolvedValue(undefined)
  const result = render(
    <DeleteUserModal user={TARGET} onClose={onClose} onDelete={onDelete} onSuccess={onSuccess} />
  )
  return { ...result, onClose, onSuccess, onDelete }
}

beforeEach(() => { vi.clearAllMocks() })

describe('DeleteUserModal', () => {
  it('displays username, email and the translated role', () => {
    const { getByText } = setup()
    expect(getByText('alice')).toBeDefined()
    expect(getByText('alice@test.com')).toBeDefined()
    expect(getByText('user.role.USER')).toBeDefined()
  })

  it('calls onDelete and onSuccess on confirm', async () => {
    const { getByText, onDelete, onSuccess } = setup()
    fireEvent.click(getByText('delete.submit'))
    await waitFor(() => expect(onDelete).toHaveBeenCalledWith('u-1'))
    expect(onSuccess).toHaveBeenCalledOnce()
  })

  it('shows server error', async () => {
    const { getByText, findByRole } = setup({
      onDelete: vi.fn().mockRejectedValue(new ServerError()),
    })
    fireEvent.click(getByText('delete.submit'))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('delete.error.server')
  })

  it('shows network error', async () => {
    const { getByText, findByRole } = setup({
      onDelete: vi.fn().mockRejectedValue(new NetworkError()),
    })
    fireEvent.click(getByText('delete.submit'))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('delete.error.network')
  })

  it('calls onClose when cancel is clicked', () => {
    const { getByText, onClose } = setup()
    fireEvent.click(getByText('delete.cancel'))
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('prevents double submit', async () => {
    let resolve!: () => void
    const onDelete = vi.fn().mockImplementation(() => new Promise<void>(r => { resolve = r }))
    const { getByText } = setup({ onDelete })
    fireEvent.click(getByText('delete.submit'))
    fireEvent.click(getByText('delete.submit'))
    resolve()
    await waitFor(() => expect(onDelete).toHaveBeenCalledOnce())
  })
})
