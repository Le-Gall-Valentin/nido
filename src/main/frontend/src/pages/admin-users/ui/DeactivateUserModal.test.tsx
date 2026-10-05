import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ForbiddenError } from '@/shared/lib'
import { AlreadyInactiveError, type AdminUser } from '@/entities/user'
import { DeactivateUserModal } from './DeactivateUserModal'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, o?: { username?: string }) => (o?.username ? `${k}:${o.username}` : k) }),
}))

const CAROL: AdminUser = {
  id: 'u-3', username: 'carol', email: 'carol@test.com', role: 'USER', isActive: true,
  createdAt: '2026-10-01T00:00:00Z', totpEnabled: false, invitation: null,
}

describe('DeactivateUserModal', () => {
  it('deactivates once confirmed, saying who will be told', async () => {
    const onDeactivate = vi.fn().mockResolvedValue(undefined)
    const onSuccess = vi.fn()
    render(<DeactivateUserModal user={CAROL} mail="available" onClose={vi.fn()} onDeactivate={onDeactivate} onSuccess={onSuccess} />)

    expect(screen.getByText('notice.mail:carol')).not.toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'deactivate.submit' }))

    await waitFor(() => expect(onDeactivate).toHaveBeenCalledWith(CAROL))
    expect(onSuccess).toHaveBeenCalledOnce()
  })

  it('stays open with the reason when it is refused', async () => {
    const onSuccess = vi.fn()
    render(<DeactivateUserModal user={CAROL} mail="available" onClose={vi.fn()}
      onDeactivate={vi.fn().mockRejectedValue(new ForbiddenError())} onSuccess={onSuccess} />)

    fireEvent.click(screen.getByRole('button', { name: 'deactivate.submit' }))

    expect(await screen.findByText('deactivate.error.forbidden')).not.toBeNull()
    expect(onSuccess).not.toHaveBeenCalled()
  })

  it('says so when another administrator deactivated the account first', async () => {
    render(<DeactivateUserModal user={CAROL} mail="available" onClose={vi.fn()}
      onDeactivate={vi.fn().mockRejectedValue(new AlreadyInactiveError())} onSuccess={vi.fn()} />)

    fireEvent.click(screen.getByRole('button', { name: 'deactivate.submit' }))

    expect(await screen.findByText('deactivate.error.already_inactive')).not.toBeNull()
  })

  it('cancels without deactivating', () => {
    const onClose = vi.fn()
    const onDeactivate = vi.fn()
    render(<DeactivateUserModal user={CAROL} mail="available" onClose={onClose} onDeactivate={onDeactivate} onSuccess={vi.fn()} />)

    fireEvent.click(screen.getByRole('button', { name: 'deactivate.cancel' }))

    expect(onClose).toHaveBeenCalledOnce()
    expect(onDeactivate).not.toHaveBeenCalled()
  })
})
