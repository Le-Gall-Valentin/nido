import { render, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { EditUserRoleModal } from './EditUserRoleModal'
import { NetworkError, ServerError } from '@/shared/lib'
import { RoleAlreadyAssignedError } from '../api/adminUsersApi'
import type { User, AdminUser } from '@/entities/user'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, string>) => {
    if (opts?.username) return `${k}:${opts.username}`
    return k
  }}),
}))

vi.mock('@/shared/ui', () => ({
  CTA_BUTTON_STYLE: {},
  Alert: ({ children, variant }: { children: React.ReactNode; variant: string }) => (
    <div role={variant === 'error' ? 'alert' : 'status'}>{children}</div>
  ),
  Dialog: ({ children, open }: { children: React.ReactNode; open: boolean }) =>
    open ? <div data-testid="dialog">{children}</div> : null,
  Button: ({ children, onClick, disabled, isLoading, type, ...props }: React.ButtonHTMLAttributes<HTMLButtonElement> & { isLoading?: boolean; children: React.ReactNode }) => (
    <button type={type} onClick={onClick} disabled={disabled || isLoading} {...props}>{children}</button>
  ),
}))

const TARGET_USER: AdminUser = {
  id: 'u-t', username: 'bob', email: 'bob@test.com',
  role: 'USER', isActive: true, createdAt: '2024-01-01T00:00:00Z', totpEnabled: false,
}
const TARGET_ADMIN: AdminUser = { ...TARGET_USER, id: 'a-t', username: 'carol', role: 'ADMIN' }
const TARGET_SUPER_ADMIN: AdminUser = { ...TARGET_USER, id: 'sa-t', username: 'root', role: 'SUPER_ADMIN' }

const SUPER_ADMIN_CALLER: User = {
  id: 'sa', username: 'sa', email: 'sa@test.com',
  role: 'SUPER_ADMIN', createdAt: '2024-01-01T00:00:00Z', totpEnabled: false,
}
const ADMIN_CALLER: User = {
  id: 'a1', username: 'admin', email: 'admin@test.com',
  role: 'ADMIN', createdAt: '2024-01-01T00:00:00Z', totpEnabled: false,
}

function setup(target: AdminUser, caller: User, overrides: { onUpdate?: () => Promise<void> } = {}) {
  const onClose = vi.fn()
  const onSuccess = vi.fn()
  const onUpdate = overrides.onUpdate ?? vi.fn().mockResolvedValue(undefined)
  const result = render(
    <EditUserRoleModal target={target} caller={caller} onClose={onClose} onUpdate={onUpdate} onSuccess={onSuccess} />
  )
  return { ...result, onClose, onSuccess, onUpdate }
}

beforeEach(() => { vi.clearAllMocks() })

describe('EditUserRoleModal — SUPER_ADMIN caller', () => {
  it('shows USER and ADMIN options, never SUPER_ADMIN', () => {
    const { getByRole } = setup(TARGET_USER, SUPER_ADMIN_CALLER)
    const select = getByRole('combobox') as HTMLSelectElement
    const options = Array.from(select.options).map(o => o.value)
    expect(options).toEqual(['USER', 'ADMIN'])
  })

  it('pre-selects current role', () => {
    const { getByRole } = setup(TARGET_ADMIN, SUPER_ADMIN_CALLER)
    const select = getByRole('combobox') as HTMLSelectElement
    expect(select.value).toBe('ADMIN')
  })

  it('submit is disabled while the role is unchanged (backend returns 409 on no-op)', () => {
    const { getByText } = setup(TARGET_USER, SUPER_ADMIN_CALLER)
    expect((getByText('edit_role.submit') as HTMLButtonElement).disabled).toBe(true)
  })

  it('submit becomes enabled when a different role is selected', () => {
    const { getByRole, getByText } = setup(TARGET_USER, SUPER_ADMIN_CALLER)
    fireEvent.change(getByRole('combobox'), { target: { value: 'ADMIN' } })
    expect((getByText('edit_role.submit') as HTMLButtonElement).disabled).toBe(false)
  })

  it('calls onUpdate and onSuccess on submit', async () => {
    const { getByText, getByRole, onUpdate, onSuccess } = setup(TARGET_USER, SUPER_ADMIN_CALLER)
    fireEvent.change(getByRole('combobox'), { target: { value: 'ADMIN' } })
    fireEvent.click(getByText('edit_role.submit'))
    await waitFor(() => expect(onUpdate).toHaveBeenCalledWith('u-t', 'ADMIN'))
    expect(onSuccess).toHaveBeenCalledWith('ADMIN')
  })

  it('prevents double submit', async () => {
    let resolve!: () => void
    const onUpdate = vi.fn().mockImplementation(() => new Promise<void>(r => { resolve = r }))
    const { getByText, getByRole } = setup(TARGET_USER, SUPER_ADMIN_CALLER, { onUpdate })
    fireEvent.change(getByRole('combobox'), { target: { value: 'ADMIN' } })
    fireEvent.click(getByText('edit_role.submit'))
    fireEvent.click(getByText('edit_role.submit'))
    resolve()
    await waitFor(() => expect(onUpdate).toHaveBeenCalledOnce())
  })

  it('keeps submit disabled for a SUPER_ADMIN target (never demotable)', () => {
    const { getByText } = setup(TARGET_SUPER_ADMIN, SUPER_ADMIN_CALLER)
    expect((getByText('edit_role.submit') as HTMLButtonElement).disabled).toBe(true)
  })
})

describe('EditUserRoleModal — ADMIN caller', () => {
  it('shows only USER option', () => {
    const { getByRole } = setup(TARGET_USER, ADMIN_CALLER)
    const select = getByRole('combobox') as HTMLSelectElement
    const options = Array.from(select.options).map(o => o.value)
    expect(options).toEqual(['USER'])
  })
})

describe('EditUserRoleModal — errors', () => {
  it('shows RoleAlreadyAssignedError when the API still reports a no-op', async () => {
    const { getByText, getByRole, findByRole } = setup(TARGET_USER, SUPER_ADMIN_CALLER, {
      onUpdate: vi.fn().mockRejectedValue(new RoleAlreadyAssignedError()),
    })
    fireEvent.change(getByRole('combobox'), { target: { value: 'ADMIN' } })
    fireEvent.click(getByText('edit_role.submit'))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('edit_role.error.already_assigned')
  })

  it('shows server error', async () => {
    const { getByText, getByRole, findByRole } = setup(TARGET_USER, SUPER_ADMIN_CALLER, {
      onUpdate: vi.fn().mockRejectedValue(new ServerError()),
    })
    fireEvent.change(getByRole('combobox'), { target: { value: 'ADMIN' } })
    fireEvent.click(getByText('edit_role.submit'))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('edit_role.error.server')
  })

  it('shows network error', async () => {
    const { getByText, getByRole, findByRole } = setup(TARGET_USER, SUPER_ADMIN_CALLER, {
      onUpdate: vi.fn().mockRejectedValue(new NetworkError()),
    })
    fireEvent.change(getByRole('combobox'), { target: { value: 'ADMIN' } })
    fireEvent.click(getByText('edit_role.submit'))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('edit_role.error.network')
  })

  it('calls onClose on cancel', () => {
    const { getByText, onClose } = setup(TARGET_USER, SUPER_ADMIN_CALLER)
    fireEvent.click(getByText('edit_role.cancel'))
    expect(onClose).toHaveBeenCalledOnce()
  })
})
