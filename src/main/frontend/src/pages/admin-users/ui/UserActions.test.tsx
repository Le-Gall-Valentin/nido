import { render, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { UserActions } from './UserActions'
import type { User, AdminUser } from '@/entities/user'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, string>) => (opts?.username ? `${k}:${opts.username}` : k) }),
}))

const SA: User = { id: 'sa', username: 'sa', email: 'sa@test.com', role: 'SUPER_ADMIN', createdAt: '2024-01-01T00:00:00Z', totpEnabled: true }
const ADMIN: User = { id: 'a1', username: 'admin', email: 'admin@test.com', role: 'ADMIN', createdAt: '2024-01-01T00:00:00Z', totpEnabled: false }

function target(overrides: Partial<AdminUser> = {}): AdminUser {
  return { id: 'u1', username: 'alice', email: 'alice@test.com', role: 'USER', isActive: true, createdAt: '2024-01-01T00:00:00Z', totpEnabled: true, ...overrides }
}

function setup(currentUser: User, user: AdminUser) {
  const handlers = { onEditRole: vi.fn(), onResetTotp: vi.fn(), onDelete: vi.fn() }
  const result = render(<UserActions user={user} currentUser={currentUser} {...handlers} />)
  return { ...result, ...handlers }
}

beforeEach(() => { vi.clearAllMocks() })

describe('UserActions — gating', () => {
  it('enables all three actions for a SUPER_ADMIN acting on a standard USER with totp', () => {
    const { getByLabelText } = setup(SA, target())
    expect((getByLabelText('table.btn_delete') as HTMLButtonElement).disabled).toBe(false)
    expect((getByLabelText('table.btn_edit:alice') as HTMLButtonElement).disabled).toBe(false)
    expect((getByLabelText('table.btn_reset_totp:alice') as HTMLButtonElement).disabled).toBe(false)
  })

  it('disables delete and edit on a SUPER_ADMIN target', () => {
    const { getByLabelText } = setup(SA, target({ id: 'sa2', username: 'root', role: 'SUPER_ADMIN' }))
    expect((getByLabelText('table.btn_delete') as HTMLButtonElement).disabled).toBe(true)
    expect((getByLabelText('table.btn_edit:root') as HTMLButtonElement).disabled).toBe(true)
  })

  it('disables totp reset when the target has no totp enabled', () => {
    const { getByLabelText } = setup(SA, target({ totpEnabled: false }))
    expect((getByLabelText('table.btn_reset_totp:alice') as HTMLButtonElement).disabled).toBe(true)
  })

  it('prevents an ADMIN from acting on another ADMIN', () => {
    const { getByLabelText } = setup(ADMIN, target({ id: 'a2', username: 'bob', role: 'ADMIN' }))
    expect((getByLabelText('table.btn_delete') as HTMLButtonElement).disabled).toBe(true)
    expect((getByLabelText('table.btn_edit:bob') as HTMLButtonElement).disabled).toBe(true)
  })
})

describe('UserActions — handlers', () => {
  it('fires the matching handler with the user when enabled', () => {
    const user = target()
    const { getByLabelText, onEditRole, onResetTotp, onDelete } = setup(SA, user)
    fireEvent.click(getByLabelText('table.btn_edit:alice'))
    fireEvent.click(getByLabelText('table.btn_reset_totp:alice'))
    fireEvent.click(getByLabelText('table.btn_delete'))
    expect(onEditRole).toHaveBeenCalledWith(user)
    expect(onResetTotp).toHaveBeenCalledWith(user)
    expect(onDelete).toHaveBeenCalledWith(user)
  })
})

describe('UserActions — sizing', () => {
  it('uses larger touch targets for the md size', () => {
    const { getByLabelText } = render(
      <UserActions user={target()} currentUser={SA} size="md" onEditRole={vi.fn()} onResetTotp={vi.fn()} onDelete={vi.fn()} />
    )
    expect(getByLabelText('table.btn_delete').className).toContain('size-9')
  })
})
