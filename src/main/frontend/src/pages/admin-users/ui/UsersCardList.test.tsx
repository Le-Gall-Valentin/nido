import { render, within } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { UsersCardList } from './UsersCardList'
import type { AdminUser } from '../api/adminUsersApi'
import type { User } from '@/entities/user'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (k: string, opts?: Record<string, string>) => (opts?.username ? `${k}:${opts.username}` : k),
    i18n: { language: 'fr' },
  }),
}))

const SA: User = { id: 'sa', username: 'superadmin', email: 'sa@test.com', role: 'SUPER_ADMIN', createdAt: '2024-01-01T00:00:00Z', totpEnabled: true }

const USERS: AdminUser[] = [
  { ...SA, isActive: true },
  { id: 'u1', username: 'testuser', email: 'test@test.com', role: 'USER', isActive: true, createdAt: '2024-02-01T00:00:00Z', totpEnabled: true },
]

const HANDLERS = {
  onToggleActive: vi.fn(),
  onEditRole: vi.fn(),
  onResetTotp: vi.fn(),
  onDelete: vi.fn(),
}

function setup(users: AdminUser[] = USERS, isLoading = false, currentUser: User = SA) {
  return render(
    <UsersCardList users={users} isLoading={isLoading} currentUser={currentUser} {...HANDLERS} />
  )
}

beforeEach(() => { vi.clearAllMocks() })

describe('UsersCardList', () => {
  it('renders skeleton cards while loading', () => {
    const { container } = setup([], true)
    expect(container.querySelectorAll('.animate-pulse').length).toBeGreaterThan(0)
  })

  it('renders the empty message when there are no users', () => {
    const { getByText } = setup([])
    expect(getByText('table.empty')).toBeDefined()
  })

  it('renders one card per user with username and email', () => {
    const { getAllByRole, getByText } = setup()
    expect(getAllByRole('listitem')).toHaveLength(USERS.length)
    expect(getByText('superadmin')).toBeDefined()
    expect(getByText('test@test.com')).toBeDefined()
  })

  it('shows the "vous" badge on the current user card', () => {
    const { getByText } = setup()
    expect(getByText('table.you')).toBeDefined()
  })

  it('renders the translated role label per card (shell namespace)', () => {
    const { getAllByRole } = setup()
    const saCard = getAllByRole('listitem').find(c => within(c).queryByText('superadmin'))!
    expect(within(saCard).getByText('user.role.SUPER_ADMIN')).toBeDefined()
  })

  it('exposes the action buttons and status toggle per card', () => {
    const { getAllByRole, getByLabelText } = setup()
    // one switch per user
    expect(getAllByRole('switch')).toHaveLength(USERS.length)
    // actions for the standard user are present
    expect(getByLabelText('table.btn_edit:testuser')).toBeDefined()
    expect(getByLabelText('table.btn_reset_totp:testuser')).toBeDefined()
  })

  it('disables delete on the SUPER_ADMIN card but enables it on the standard user', () => {
    const { getAllByRole } = setup()
    const cards = getAllByRole('listitem')
    // Locate each card by its identity rather than relying on render order.
    const saCard = cards.find(c => within(c).queryByText('superadmin'))!
    const userCard = cards.find(c => within(c).queryByText('testuser'))!
    const saDelete = within(saCard).getByLabelText('table.btn_delete') as HTMLButtonElement
    const userDelete = within(userCard).getByLabelText('table.btn_delete') as HTMLButtonElement
    expect(saDelete.disabled).toBe(true)
    expect(userDelete.disabled).toBe(false)
  })
})
