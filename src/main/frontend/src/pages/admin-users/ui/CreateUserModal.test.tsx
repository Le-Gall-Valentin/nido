import { render, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { CreateUserModal } from './CreateUserModal'
import { NetworkError, ServerError } from '@/shared/lib'
import { ConflictError } from '../api/adminUsersApi'
import type { User } from '@/entities/user'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
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
  Input: ({ label, name, type = 'text', value, onChange, disabled, placeholder, autoFocus }: {
    label: string; name: string; type?: string; value: string;
    onChange: React.ChangeEventHandler<HTMLInputElement>; disabled?: boolean; placeholder?: string; autoFocus?: boolean
  }) => (
    <div>
      <label htmlFor={name}>{label}</label>
      <input id={name} name={name} type={type} value={value} onChange={onChange} disabled={disabled} placeholder={placeholder} autoFocus={autoFocus} />
    </div>
  ),
}))

const SUPER_ADMIN_CALLER: User = {
  id: 'sa', username: 'sa', email: 'sa@test.com',
  role: 'SUPER_ADMIN', createdAt: '2024-01-01T00:00:00Z', totpEnabled: false,
}
const ADMIN_CALLER: User = { ...SUPER_ADMIN_CALLER, id: 'a1', username: 'admin', role: 'ADMIN' }

const VALID_PASSWORD = 'P@ssword1!'

function setup(overrides: { onCreate?: () => Promise<void>; caller?: User } = {}) {
  const onClose = vi.fn()
  const onSuccess = vi.fn()
  const onCreate = overrides.onCreate ?? vi.fn().mockResolvedValue(undefined)
  const result = render(
    <CreateUserModal caller={overrides.caller ?? SUPER_ADMIN_CALLER} onClose={onClose} onCreate={onCreate} onSuccess={onSuccess} />
  )
  return { ...result, onClose, onSuccess, onCreate }
}

function fillForm(getByLabelText: ReturnType<typeof render>['getByLabelText'], password = VALID_PASSWORD) {
  fireEvent.change(getByLabelText('create.username'), { target: { value: 'bob' } })
  fireEvent.change(getByLabelText('create.email'), { target: { value: 'bob@test.com' } })
  fireEvent.change(getByLabelText('create.password'), { target: { value: password } })
}

beforeEach(() => { vi.clearAllMocks() })

describe('CreateUserModal — role options', () => {
  it('SUPER_ADMIN caller sees USER and ADMIN options', () => {
    const { getByRole } = setup()
    const select = getByRole('combobox') as HTMLSelectElement
    expect(Array.from(select.options).map(o => o.value)).toEqual(['USER', 'ADMIN'])
  })

  it('ADMIN caller sees USER only (backend RoleHierarchy)', () => {
    const { getByRole } = setup({ caller: ADMIN_CALLER })
    const select = getByRole('combobox') as HTMLSelectElement
    expect(Array.from(select.options).map(o => o.value)).toEqual(['USER'])
  })
})

describe('CreateUserModal — validation', () => {
  it('submit button is disabled when fields are empty', () => {
    const { getByText } = setup()
    expect((getByText('create.submit') as HTMLButtonElement).disabled).toBe(true)
  })

  it('submit stays disabled with a weak password and shows hint', () => {
    const { getByLabelText, getByText } = setup()
    fillForm(getByLabelText, 'weakpassword')
    expect((getByText('create.submit') as HTMLButtonElement).disabled).toBe(true)
    expect(getByText('create.error.password_weak')).toBeDefined()
  })

  it('submit stays disabled with a too short password and shows hint', () => {
    const { getByLabelText, getByText } = setup()
    fillForm(getByLabelText, 'P@s1')
    expect((getByText('create.submit') as HTMLButtonElement).disabled).toBe(true)
    expect(getByText('create.error.password_too_short')).toBeDefined()
  })

  it('shows a too-long hint (not the weak hint) for a password over 72 chars', () => {
    const { getByLabelText, getByText, queryByText } = setup()
    // Otherwise-valid (upper/digit/special) but longer than the 72-char limit.
    fillForm(getByLabelText, `P@ssw0rd${'a'.repeat(72)}`)
    expect((getByText('create.submit') as HTMLButtonElement).disabled).toBe(true)
    expect(getByText('create.error.password_too_long')).toBeDefined()
    expect(queryByText('create.error.password_weak')).toBeNull()
  })

  it('shows hint when username is shorter than 3 chars', () => {
    const { getByLabelText, getByText } = setup()
    fireEvent.change(getByLabelText('create.username'), { target: { value: 'ab' } })
    expect(getByText('create.error.username_length')).toBeDefined()
  })

  it('shows an email-invalid hint and keeps submit disabled for a malformed email', () => {
    const { getByLabelText, getByText } = setup()
    fireEvent.change(getByLabelText('create.username'), { target: { value: 'bob' } })
    fireEvent.change(getByLabelText('create.email'), { target: { value: 'notanemail' } })
    fireEvent.change(getByLabelText('create.password'), { target: { value: VALID_PASSWORD } })
    expect(getByText('create.error.email_invalid')).toBeDefined()
    expect((getByText('create.submit') as HTMLButtonElement).disabled).toBe(true)
  })

  it('calls onCreate with trimmed values and selected role on submit', async () => {
    const { getByLabelText, getByText, onCreate, onSuccess } = setup()
    fillForm(getByLabelText)
    fireEvent.click(getByText('create.submit'))
    await waitFor(() => expect(onCreate).toHaveBeenCalledWith('bob', 'bob@test.com', VALID_PASSWORD, 'USER'))
    expect(onSuccess).toHaveBeenCalledOnce()
  })

  it('trims whitespace from username and email', async () => {
    const { getByLabelText, getByText, onCreate } = setup()
    fireEvent.change(getByLabelText('create.username'), { target: { value: '  bob  ' } })
    fireEvent.change(getByLabelText('create.email'), { target: { value: '  bob@test.com  ' } })
    fireEvent.change(getByLabelText('create.password'), { target: { value: VALID_PASSWORD } })
    fireEvent.click(getByText('create.submit'))
    await waitFor(() => expect(onCreate).toHaveBeenCalledWith('bob', 'bob@test.com', VALID_PASSWORD, 'USER'))
  })
})

describe('CreateUserModal — errors', () => {
  it('shows conflict error on 409', async () => {
    const { getByLabelText, getByText, findByRole } = setup({
      onCreate: vi.fn().mockRejectedValue(new ConflictError()),
    })
    fillForm(getByLabelText)
    fireEvent.click(getByText('create.submit'))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('create.error.conflict')
  })

  it('shows server error', async () => {
    const { getByLabelText, getByText, findByRole } = setup({
      onCreate: vi.fn().mockRejectedValue(new ServerError()),
    })
    fillForm(getByLabelText)
    fireEvent.click(getByText('create.submit'))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('create.error.server')
  })

  it('shows network error', async () => {
    const { getByLabelText, getByText, findByRole } = setup({
      onCreate: vi.fn().mockRejectedValue(new NetworkError()),
    })
    fillForm(getByLabelText)
    fireEvent.click(getByText('create.submit'))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('create.error.network')
  })

  it('calls onClose when cancel is clicked', () => {
    const { getByText, onClose } = setup()
    fireEvent.click(getByText('create.cancel'))
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('prevents double submit', async () => {
    let resolve!: () => void
    const onCreate = vi.fn().mockImplementation(() => new Promise<void>(r => { resolve = r }))
    const { getByLabelText, getByText } = setup({ onCreate })
    fillForm(getByLabelText)
    fireEvent.click(getByText('create.submit'))
    fireEvent.click(getByText('create.submit'))
    resolve()
    await waitFor(() => expect(onCreate).toHaveBeenCalledOnce())
  })

  it('clears the error alert on a successful resubmit', async () => {
    const onCreate = vi.fn()
      .mockRejectedValueOnce(new ServerError())
      .mockResolvedValueOnce(undefined)
    const { getByLabelText, getByText, findByRole, queryByRole } = setup({ onCreate })
    fillForm(getByLabelText)
    fireEvent.click(getByText('create.submit'))
    expect((await findByRole('alert')).textContent).toContain('create.error.server')
    fireEvent.click(getByText('create.submit'))
    await waitFor(() => expect(queryByRole('alert')).toBeNull())
  })
})
