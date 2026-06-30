import { render, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { UserStatusToggle } from './UserStatusToggle'
import type { User, AdminUser } from '@/entities/user'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

const SA: User = { id: 'sa', username: 'sa', email: 'sa@test.com', role: 'SUPER_ADMIN', createdAt: '2024-01-01T00:00:00Z', totpEnabled: true }

function target(overrides: Partial<AdminUser> = {}): AdminUser {
  return { id: 'u1', username: 'alice', email: 'alice@test.com', role: 'USER', isActive: true, createdAt: '2024-01-01T00:00:00Z', totpEnabled: false, ...overrides }
}

function setup(currentUser: User, user: AdminUser) {
  const onToggle = vi.fn()
  return { ...render(<UserStatusToggle user={user} currentUser={currentUser} onToggle={onToggle} />), onToggle }
}

beforeEach(() => { vi.clearAllMocks() })

describe('UserStatusToggle', () => {
  it('reflects the active state via aria-checked', () => {
    const { getByRole } = setup(SA, target({ isActive: true }))
    expect(getByRole('switch').getAttribute('aria-checked')).toBe('true')
  })

  it('uses the deactivate label when active', () => {
    const { getByRole } = setup(SA, target({ isActive: true }))
    expect(getByRole('switch').getAttribute('aria-label')).toBe('table.toggle_deactivate')
  })

  it('uses the activate label when inactive', () => {
    const { getByRole } = setup(SA, target({ isActive: false }))
    expect(getByRole('switch').getAttribute('aria-label')).toBe('table.toggle_activate')
  })

  it('calls onToggle when the action is allowed', () => {
    const user = target()
    const { getByRole, onToggle } = setup(SA, user)
    fireEvent.click(getByRole('switch'))
    expect(onToggle).toHaveBeenCalledWith(user)
  })

  it('is disabled and does not call onToggle on a SUPER_ADMIN target', () => {
    const { getByRole, onToggle } = setup(SA, target({ id: 'sa2', role: 'SUPER_ADMIN' }))
    const sw = getByRole('switch') as HTMLButtonElement
    expect(sw.disabled).toBe(true)
    fireEvent.click(sw)
    expect(onToggle).not.toHaveBeenCalled()
  })

  it('is disabled on self', () => {
    const { getByRole } = setup(SA, target({ id: 'sa', role: 'SUPER_ADMIN' }))
    expect((getByRole('switch') as HTMLButtonElement).disabled).toBe(true)
  })

  it('is disabled and ignores clicks while the toggle is in flight', () => {
    const user = target()
    const onToggle = vi.fn()
    const { getByRole } = render(
      <UserStatusToggle user={user} currentUser={SA} onToggle={onToggle} isPending />
    )
    const sw = getByRole('switch') as HTMLButtonElement
    expect(sw.disabled).toBe(true)
    fireEvent.click(sw)
    expect(onToggle).not.toHaveBeenCalled()
  })
})
