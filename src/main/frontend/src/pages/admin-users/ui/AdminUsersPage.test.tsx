import { fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { AdminUsersPage } from './AdminUsersPage'
import { renderWithQuery } from '@/shared/test'
import type { AdminUser, UsersPage, User } from '@/entities/user'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, string | number>) => {
    if (opts && typeof opts.count === 'number') return `${k}:${opts.count}`
    if (opts && opts.current) return `${k}:${opts.current}/${opts.total}`
    return k
  }}),
  Trans: ({ i18nKey }: { i18nKey: string }) => i18nKey,
}))

const mockUseAuth = vi.hoisted(() => vi.fn())
const mockApi = vi.hoisted(() => ({
  listUsers: vi.fn(),
  createUser: vi.fn(),
  updateUserRole: vi.fn(),
  activateUser: vi.fn(),
  deactivateUser: vi.fn(),
  resetTotp: vi.fn(),
  deleteUser: vi.fn(),
    resendInvitation: vi.fn(),
}))

vi.mock('@/features/auth', () => ({
  useAuth: (...args: unknown[]) => mockUseAuth(...args),
}))

vi.mock('@/features/password-reset', () => ({ useMailAvailability: () => 'available' }))

// The concrete api is injected through the slice's DIP context, so the fake is
// provided via AdminUsersApiProvider rather than module-mocked.

vi.mock('./UsersTable', () => ({
  UsersTable: (props: {
    users: AdminUser[]
    isLoading: boolean
    onEditRole: (u: AdminUser) => void
    onDelete: (u: AdminUser) => void
    onResetTotp: (u: AdminUser) => void
    onToggleActive: (u: AdminUser) => void
    onResendInvitation: (u: AdminUser) => void
  }) => {
    if (props.isLoading) return <div data-testid="skeleton" />
    return (
      <div data-testid="users-table">
        <button data-testid="trigger-edit" onClick={() => props.onEditRole(MOCK_USER)}>edit</button>
        <button data-testid="trigger-delete" onClick={() => props.onDelete(MOCK_USER)}>delete</button>
        <button data-testid="trigger-totp" onClick={() => props.onResetTotp(MOCK_USER)}>totp</button>
        <button data-testid="trigger-toggle" onClick={() => props.onToggleActive(props.users[0])}>toggle</button>
        <button data-testid="trigger-resend" onClick={() => props.onResendInvitation(MOCK_USER)}>resend</button>
      </div>
    )
  },
}))

// The page renders both layouts (CSS-toggled); the table mock drives the
// orchestration assertions, so the card list is stubbed out here.
vi.mock('./UsersCardList', () => ({
  UsersCardList: () => null,
}))

vi.mock('./CreateUserModal', () => ({
  CreateUserModal: ({ onClose }: { onClose: () => void }) => (
    <div data-testid="create-modal">
      <button onClick={onClose}>close-create</button>
    </div>
  ),
}))

vi.mock('./EditUserRoleModal', () => ({
  EditUserRoleModal: ({ onClose }: { onClose: () => void }) => (
    <div data-testid="edit-modal"><button onClick={onClose}>close-edit</button></div>
  ),
}))

vi.mock('./DeleteUserModal', () => ({
  DeleteUserModal: ({ onClose, onSuccess }: { onClose: () => void; onSuccess: () => void }) => (
    <div data-testid="delete-modal">
      <button onClick={onClose}>close-delete</button>
      <button onClick={onSuccess}>success-delete</button>
    </div>
  ),
}))

vi.mock('./DeactivateUserModal', () => ({
  DeactivateUserModal: ({ user, onDeactivate, onSuccess }: {
    user: AdminUser; onDeactivate: (u: AdminUser) => Promise<void>; onSuccess: () => void
  }) => (
    <div data-testid="deactivate-modal">
      <button data-testid="confirm-deactivate" onClick={() => { void onDeactivate(user).then(onSuccess, () => {}) }}>confirm</button>
    </div>
  ),
}))

vi.mock('./ResendInvitationModal', () => ({
  ResendInvitationModal: ({ onClose }: { onClose: () => void }) => (
    <div data-testid="resend-modal"><button onClick={onClose}>close-resend</button></div>
  ),
}))

vi.mock('./ResetTotpModal', () => ({
  ResetTotpModal: ({ onClose }: { onClose: () => void }) => (
    <div data-testid="totp-modal"><button onClick={onClose}>close-totp</button></div>
  ),
}))

const MOCK_CURRENT_USER: User = {
  id: 'sa', username: 'superadmin', email: 'sa@test.com',
  role: 'SUPER_ADMIN', createdAt: '2024-01-01T00:00:00Z', totpEnabled: true,
}
const MOCK_USER: AdminUser = {
  id: 'u1', username: 'alice', email: 'alice@test.com',
  role: 'USER', isActive: true, invitation: null, createdAt: '2024-02-01T00:00:00Z', totpEnabled: false,
}
const MOCK_PAGE: UsersPage = {
  content: [MOCK_USER],
  totalElements: 1,
  page: 0,
  size: 20,
}

function setup() {
  mockUseAuth.mockImplementation((selector: (s: { user: User }) => unknown) =>
    selector({ user: MOCK_CURRENT_USER })
  )
  return renderWithQuery(<AdminUsersPage api={mockApi} />)
}

beforeEach(() => {
  vi.clearAllMocks()
  mockApi.listUsers.mockResolvedValue(MOCK_PAGE)
  mockApi.deactivateUser.mockResolvedValue(undefined)
  mockApi.activateUser.mockResolvedValue(undefined)
  mockApi.deleteUser.mockResolvedValue(undefined)
  mockApi.resetTotp.mockResolvedValue(undefined)
  mockApi.updateUserRole.mockResolvedValue(undefined)
  mockApi.createUser.mockResolvedValue({ delivery: 'mail' })
  mockApi.resendInvitation.mockResolvedValue({ delivery: 'mail' })
})

describe('AdminUsersPage — loading', () => {
  it('shows skeleton while fetching', () => {
    mockApi.listUsers.mockReturnValue(new Promise(() => {}))
    const { getByTestId } = setup()
    expect(getByTestId('skeleton')).toBeDefined()
  })

  it('shows table after fetch resolves', async () => {
    const { findByTestId } = setup()
    expect(await findByTestId('users-table')).toBeDefined()
  })
})

describe('AdminUsersPage — load error', () => {
  it('shows load_error alert when listUsers rejects', async () => {
    mockApi.listUsers.mockRejectedValue(new Error('fail'))
    const { findByRole } = setup()
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('load_error')
  })
})

describe('AdminUsersPage — create modal', () => {
  it('opens CreateUserModal when header button clicked', async () => {
    const { findByTestId, getByText } = setup()
    await findByTestId('users-table')
    fireEvent.click(getByText('action.create'))
    expect(await findByTestId('create-modal')).toBeDefined()
  })

  it('closes modal when onClose is called', async () => {
    const { findByTestId, getByText, queryByTestId } = setup()
    await findByTestId('users-table')
    fireEvent.click(getByText('action.create'))
    await findByTestId('create-modal')
    fireEvent.click(getByText('close-create'))
    await waitFor(() => expect(queryByTestId('create-modal')).toBeNull())
  })
})

describe('AdminUsersPage — edit modal', () => {
  it('opens EditUserRoleModal when edit triggered', async () => {
    const { findByTestId } = setup()
    await findByTestId('users-table')
    fireEvent.click(document.querySelector('[data-testid="trigger-edit"]')!)
    expect(await findByTestId('edit-modal')).toBeDefined()
  })
})

describe('AdminUsersPage — delete modal', () => {
  it('opens DeleteUserModal when delete triggered', async () => {
    const { findByTestId } = setup()
    await findByTestId('users-table')
    fireEvent.click(document.querySelector('[data-testid="trigger-delete"]')!)
    expect(await findByTestId('delete-modal')).toBeDefined()
  })

  it('closes delete modal after onSuccess', async () => {
    const { findByTestId, queryByTestId } = setup()
    await findByTestId('users-table')
    fireEvent.click(document.querySelector('[data-testid="trigger-delete"]')!)
    await findByTestId('delete-modal')
    fireEvent.click(document.querySelector('[data-testid="delete-modal"] button:last-child')!)
    await waitFor(() => expect(queryByTestId('delete-modal')).toBeNull())
  })
})

describe('AdminUsersPage — reset totp modal', () => {
  it('opens ResetTotpModal when totp triggered', async () => {
    const { findByTestId } = setup()
    await findByTestId('users-table')
    fireEvent.click(document.querySelector('[data-testid="trigger-totp"]')!)
    expect(await findByTestId('totp-modal')).toBeDefined()
  })
})

describe('AdminUsersPage — resend invitation modal', () => {
  it('opens ResendInvitationModal when resend triggered', async () => {
    const { findByTestId } = setup()
    await findByTestId('users-table')
    fireEvent.click(document.querySelector('[data-testid="trigger-resend"]')!)
    expect(await findByTestId('resend-modal')).not.toBeNull()
  })
})

describe('AdminUsersPage — search', () => {
  it('fetches without search initially', async () => {
    const { findByTestId } = setup()
    await findByTestId('users-table')
    expect(mockApi.listUsers).toHaveBeenCalledWith(0, 20, undefined)
  })

  it('refetches with the debounced search term on page 0', async () => {
    const { findByTestId, getByRole } = setup()
    await findByTestId('users-table')
    fireEvent.change(getByRole('searchbox'), { target: { value: 'alice' } })
    await waitFor(() => expect(mockApi.listUsers).toHaveBeenCalledWith(0, 20, 'alice'))
  })

  it('clear button resets the search', async () => {
    const { findByTestId, getByRole } = setup()
    await findByTestId('users-table')
    fireEvent.change(getByRole('searchbox'), { target: { value: 'alice' } })
    await waitFor(() => expect(mockApi.listUsers).toHaveBeenCalledWith(0, 20, 'alice'))
    fireEvent.click(getByRole('button', { name: 'search.clear' }))
    await waitFor(() => expect((getByRole('searchbox') as HTMLInputElement).value).toBe(''))
  })
})

describe('AdminUsersPage — activation toggle', () => {
  it('asks for confirmation before deactivating an active user', async () => {
    const { findByTestId } = setup()
    await findByTestId('users-table')

    fireEvent.click(document.querySelector('[data-testid="trigger-toggle"]')!)
    expect(await findByTestId('deactivate-modal')).not.toBeNull()
    expect(mockApi.deactivateUser).not.toHaveBeenCalled()

    fireEvent.click(document.querySelector('[data-testid="confirm-deactivate"]')!)
    await waitFor(() => expect(mockApi.deactivateUser).toHaveBeenCalledWith('u1'))
  })

  it('reactivates an inactive user at once', async () => {
    mockApi.listUsers.mockResolvedValue({ ...MOCK_PAGE, content: [{ ...MOCK_USER, isActive: false }] })
    const { findByTestId } = setup()
    await findByTestId('users-table')

    fireEvent.click(document.querySelector('[data-testid="trigger-toggle"]')!)

    await waitFor(() => expect(mockApi.activateUser).toHaveBeenCalledWith('u1'))
  })

  it('shows mutation_error banner when a reactivation fails', async () => {
    mockApi.listUsers.mockResolvedValue({ ...MOCK_PAGE, content: [{ ...MOCK_USER, isActive: false }] })
    mockApi.activateUser.mockRejectedValue(new Error('fail'))
    const { findByTestId, findByRole } = setup()
    await findByTestId('users-table')

    fireEvent.click(document.querySelector('[data-testid="trigger-toggle"]')!)

    expect((await findByRole('alert')).textContent).toContain('mutation_error')
  })
})
