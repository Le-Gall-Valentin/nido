import { render, within } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { UsersTable } from './UsersTable'
import type { AdminUser , User } from '@/entities/user'
import type { MailAvailability } from '@/entities/capabilities'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, string>) => {
    if (opts?.username) return `${k}:${opts.username}`
    return k
  }, i18n: { language: 'fr' } }),
}))

vi.mock('@/entities/user', async (importActual) => {
  const actual = await importActual<typeof import('@/entities/user')>()
  return { ...actual }
})

const SA: User = { id: 'sa', username: 'superadmin', email: 'sa@test.com', role: 'SUPER_ADMIN', createdAt: '2024-01-01T00:00:00Z', twoFactorMethods: ['APP'] }
const ADMIN: User = { id: 'a1', username: 'adminuser', email: 'admin@test.com', role: 'ADMIN', createdAt: '2024-01-01T00:00:00Z', twoFactorMethods: [] }

const USERS: AdminUser[] = [
  { ...SA, isActive: true, invitation: null },
  { ...ADMIN, isActive: true, invitation: null },
  { id: 'u1', username: 'testuser', email: 'test@test.com', role: 'USER', isActive: true, invitation: null, createdAt: '2024-02-01T00:00:00Z', twoFactorMethods: ['APP'] },
  { id: 'u2', username: 'inactive', email: 'inactive@test.com', role: 'USER', isActive: false, invitation: null, createdAt: '2024-03-01T00:00:00Z', twoFactorMethods: [] },
]

const DEFAULT_HANDLERS = {
  onToggleActive: vi.fn(),
  onEditRole: vi.fn(),
  onResetTwoFactor: vi.fn(),
  onDelete: vi.fn(),
  onResendInvitation: vi.fn(),
}

function setup(currentUser: User = SA, users: AdminUser[] = USERS, isLoading = false, mail: MailAvailability = 'available') {
  return render(
    <UsersTable
      users={users}
      isLoading={isLoading}
      currentUser={currentUser}
      mail={mail}
      {...DEFAULT_HANDLERS}
    />
  )
}

beforeEach(() => { vi.clearAllMocks() })

describe('UsersTable — loading', () => {
  it('renders skeleton rows when isLoading', () => {
    const { container } = setup(SA, [], true)
    const skeletonRows = container.querySelectorAll('.animate-pulse')
    expect(skeletonRows.length).toBeGreaterThan(0)
  })

  it('does not render table when isLoading', () => {
    const { queryByRole } = setup(SA, [], true)
    expect(queryByRole('table')).toBeNull()
  })
})

describe('UsersTable — empty state', () => {
  it('shows the empty message when there are no users', () => {
    const { getByText } = setup(SA, [])
    expect(getByText('table.empty')).toBeDefined()
  })

  it('does not show the empty message when users exist', () => {
    const { queryByText } = setup()
    expect(queryByText('table.empty')).toBeNull()
  })
})

describe('UsersTable — rows', () => {
  it('renders a row for each user', () => {
    const { getAllByRole } = setup()
    const rows = getAllByRole('row')
    expect(rows.length).toBe(USERS.length + 1) // +1 for header
  })

  it('shows "vous" badge for the current user', () => {
    const { getByText } = setup(SA)
    expect(getByText(/table\.you/)).toBeDefined()
  })

  it('renders the translated role label per row (shell namespace)', () => {
    const { getAllByRole } = setup()
    const rows = getAllByRole('row').slice(1) // skip header
    expect(within(rows[0]).getByText('user.role.SUPER_ADMIN')).toBeDefined()
    expect(within(rows[2]).getByText('user.role.USER')).toBeDefined()
  })

  it('shows the app chip on the row of a user with the app on', () => {
    const { getAllByRole } = setup()
    const rows = getAllByRole('row').slice(1) // skip header
    const userRow = rows[2] // testuser, twoFactorMethods: ['APP']
    expect(within(userRow).getByText('table.two_factor_app')).toBeDefined()
    expect(within(userRow).queryByText('table.two_factor_none')).toBeNull()
  })

  it('shows no method on the row of a user without one', () => {
    const { getAllByRole } = setup()
    const rows = getAllByRole('row').slice(1)
    const inactiveRow = rows[3] // inactive, twoFactorMethods: []
    expect(within(inactiveRow).getByText('table.two_factor_none')).toBeDefined()
    expect(within(inactiveRow).queryByText('table.two_factor_app')).toBeNull()
  })
})

describe('UsersTable — button permissions', () => {
  it('delete button is disabled for SUPER_ADMIN target', () => {
    const { getAllByRole } = setup(SA)
    const rows = getAllByRole('row').slice(1) // skip header
    const saRow = rows[0]
    const deleteBtn = saRow.querySelector('[aria-label="table.btn_delete"]') as HTMLButtonElement
    expect(deleteBtn?.disabled).toBe(true)
  })

  it('delete button is enabled for USER target when caller is SUPER_ADMIN', () => {
    const { getAllByRole } = setup(SA)
    const rows = getAllByRole('row').slice(1)
    const userRow = rows[2] // testuser
    const deleteBtn = userRow.querySelector('[aria-label^="table.btn_delete"]') as HTMLButtonElement
    expect(deleteBtn?.disabled).toBe(false)
  })

  it('edit button is disabled for SUPER_ADMIN target', () => {
    const { getAllByRole } = setup(SA)
    const rows = getAllByRole('row').slice(1)
    const saRow = rows[0]
    const editBtn = saRow.querySelector('[aria-label^="table.btn_edit"]') as HTMLButtonElement
    expect(editBtn?.disabled).toBe(true)
  })

  it('the 2FA reset button is disabled when no method is on', () => {
    const { getAllByRole } = setup(SA)
    const rows = getAllByRole('row').slice(1)
    const adminRow = rows[1] // adminuser, twoFactorMethods: []
    const resetButton = adminRow.querySelector('[aria-label^="table.btn_reset_two_factor"]') as HTMLButtonElement
    expect(resetButton?.disabled).toBe(true)
  })

  it('ADMIN caller cannot edit ADMIN target', () => {
    const { getAllByRole } = setup(ADMIN)
    const rows = getAllByRole('row').slice(1)
    const saRow = rows[0] // SUPER_ADMIN
    const editBtn = saRow.querySelector('[aria-label^="table.btn_edit"]') as HTMLButtonElement
    expect(editBtn?.disabled).toBe(true)
  })
})

describe('UsersTable — invitations', () => {
  it('names the row action after how the new link will leave', () => {
    const invited: AdminUser = { ...USERS[2], invitation: { status: 'pending', expiresAt: '2026-10-12T00:00:00Z' } }
    const { getByLabelText } = setup(SA, [invited], false, 'unavailable')
    expect(getByLabelText('table.btn_new_link:testuser')).not.toBeNull()
  })
})
