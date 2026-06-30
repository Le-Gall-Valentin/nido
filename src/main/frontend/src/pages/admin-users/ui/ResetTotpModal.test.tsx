import { render, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ResetTotpModal } from './ResetTotpModal'
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
  role: 'USER', isActive: true, createdAt: '2024-01-01T00:00:00Z', totpEnabled: true,
}

function setup(overrides: { onReset?: () => Promise<void> } = {}) {
  const onClose = vi.fn()
  const onSuccess = vi.fn()
  const onReset = overrides.onReset ?? vi.fn().mockResolvedValue(undefined)
  const result = render(
    <ResetTotpModal user={TARGET} onClose={onClose} onReset={onReset} onSuccess={onSuccess} />
  )
  return { ...result, onClose, onSuccess, onReset }
}

beforeEach(() => { vi.clearAllMocks() })

describe('ResetTotpModal', () => {
  it('displays warning text', () => {
    const { getByText } = setup()
    expect(getByText('reset_totp.warning')).toBeDefined()
  })

  it('calls onReset and onSuccess on confirm', async () => {
    const { getByText, onReset, onSuccess } = setup()
    fireEvent.click(getByText('reset_totp.submit'))
    await waitFor(() => expect(onReset).toHaveBeenCalledWith('u-1'))
    expect(onSuccess).toHaveBeenCalledOnce()
  })

  it('shows server error', async () => {
    const { getByText, findByRole } = setup({
      onReset: vi.fn().mockRejectedValue(new ServerError()),
    })
    fireEvent.click(getByText('reset_totp.submit'))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('reset_totp.error.server')
  })

  it('shows network error', async () => {
    const { getByText, findByRole } = setup({
      onReset: vi.fn().mockRejectedValue(new NetworkError()),
    })
    fireEvent.click(getByText('reset_totp.submit'))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('reset_totp.error.network')
  })

  it('calls onClose when cancel is clicked', () => {
    const { getByText, onClose } = setup()
    fireEvent.click(getByText('reset_totp.cancel'))
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('prevents double submit', async () => {
    let resolve!: () => void
    const onReset = vi.fn().mockImplementation(() => new Promise<void>(r => { resolve = r }))
    const { getByText } = setup({ onReset })
    fireEvent.click(getByText('reset_totp.submit'))
    fireEvent.click(getByText('reset_totp.submit'))
    resolve()
    await waitFor(() => expect(onReset).toHaveBeenCalledOnce())
  })
})
