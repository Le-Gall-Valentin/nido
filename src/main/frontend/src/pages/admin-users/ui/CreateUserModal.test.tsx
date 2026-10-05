import { render, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import type { ComponentProps } from 'react'
import { CreateUserModal } from './CreateUserModal'
import { NetworkError, ServerError } from '@/shared/lib'
import { ConflictError } from '@/entities/user'
import type { User } from '@/entities/user'

type CreateUserModalProps = ComponentProps<typeof CreateUserModal>

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

vi.mock('@/shared/ui', async (importOriginal) => ({
  // The real field attributes: the tests read them off the rendered input.
  VERBATIM_INPUT_PROPS: (await importOriginal<typeof import('@/shared/ui')>()).VERBATIM_INPUT_PROPS,
  CTA_BUTTON_STYLE: {},
  Alert: ({ children, variant }: { children: React.ReactNode; variant: string }) => (
    <div role={variant === 'error' ? 'alert' : 'status'}>{children}</div>
  ),
  Dialog: ({ children, open }: { children: React.ReactNode; open: boolean }) =>
    open ? <div data-testid="dialog">{children}</div> : null,
  Button: ({ children, onClick, disabled, isLoading, type, ...props }: React.ButtonHTMLAttributes<HTMLButtonElement> & { isLoading?: boolean; children: React.ReactNode }) => (
    <button type={type} onClick={onClick} disabled={disabled || isLoading} {...props}>{children}</button>
  ),
  Input: ({ label, name, type = 'text', ...inputProps }: React.InputHTMLAttributes<HTMLInputElement> & { label: string; name: string }) => (
    <div>
      <label htmlFor={name}>{label}</label>
      <input id={name} name={name} type={type} {...inputProps} />
    </div>
  ),
}))

vi.mock('./InvitationResult', () => ({
  InvitationResult: ({ delivery }: { delivery: { delivery: string } }) => <div data-testid="invitation-result">{delivery.delivery}</div>,
}))

const SUPER_ADMIN_CALLER: User = {
  id: 'sa', username: 'sa', email: 'sa@test.com',
  role: 'SUPER_ADMIN', createdAt: '2024-01-01T00:00:00Z', totpEnabled: false,
}
const ADMIN_CALLER: User = { ...SUPER_ADMIN_CALLER, id: 'a1', username: 'admin', role: 'ADMIN' }

function setup(overrides: { onCreate?: CreateUserModalProps['onCreate']; caller?: User } = {}) {
  const onClose = vi.fn()
  const onSuccess = vi.fn()
  const onCreate = overrides.onCreate ?? vi.fn<CreateUserModalProps['onCreate']>().mockResolvedValue({ delivery: 'mail' })
  const result = render(
    <CreateUserModal caller={overrides.caller ?? SUPER_ADMIN_CALLER} onClose={onClose} onCreate={onCreate} onSuccess={onSuccess} />
  )
  return { ...result, onClose, onSuccess, onCreate }
}

function fillForm(getByLabelText: ReturnType<typeof render>['getByLabelText']) {
  fireEvent.change(getByLabelText('create.username'), { target: { value: 'bob' } })
  fireEvent.change(getByLabelText('create.email'), { target: { value: 'bob@test.com' } })
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

  it('shows hint when username is shorter than 3 chars', () => {
    const { getByLabelText, getByText } = setup()
    fireEvent.change(getByLabelText('create.username'), { target: { value: 'ab' } })
    expect(getByText('create.error.username_length')).toBeDefined()
  })

  it('takes the username as typed — no capital or correction from a phone keyboard', () => {
    const { getByLabelText } = setup()
    const field = getByLabelText('create.username') as HTMLInputElement
    expect(field.getAttribute('autocapitalize')).toBe('off')
    expect(field.getAttribute('autocorrect')).toBe('off')
    expect(field.getAttribute('spellcheck')).toBe('false')
  })

  it('shows a hint and keeps submit disabled when the username holds an @', () => {
    const { getByLabelText, getByText } = setup()
    fillForm(getByLabelText)
    fireEvent.change(getByLabelText('create.username'), { target: { value: 'bob@home' } })
    expect(getByText('create.error.username_at')).toBeDefined()
    expect((getByText('create.submit') as HTMLButtonElement).disabled).toBe(true)
  })

  it('shows an email-invalid hint and keeps submit disabled for a malformed email', () => {
    const { getByLabelText, getByText } = setup()
    fireEvent.change(getByLabelText('create.username'), { target: { value: 'bob' } })
    fireEvent.change(getByLabelText('create.email'), { target: { value: 'notanemail' } })
    expect(getByText('create.error.email_invalid')).toBeDefined()
    expect((getByText('create.submit') as HTMLButtonElement).disabled).toBe(true)
  })

  it('calls onCreate with trimmed values and selected role on submit', async () => {
    const { getByLabelText, getByText, onCreate } = setup()
    fillForm(getByLabelText)
    fireEvent.click(getByText('create.submit'))
    await waitFor(() => expect(onCreate).toHaveBeenCalledWith('bob', 'bob@test.com', 'USER'))
  })

  it('shows how the invitation left, and calls onSuccess only when closed', async () => {
    const { getByLabelText, getByRole, findByTestId, onSuccess } = setup()
    fillForm(getByLabelText)

    fireEvent.click(getByRole('button', { name: 'create.submit' }))

    expect((await findByTestId('invitation-result')).textContent).toBe('mail')
    expect(onSuccess).not.toHaveBeenCalled()
    fireEvent.click(getByRole('button', { name: 'invitation.done' }))
    expect(onSuccess).toHaveBeenCalledOnce()
  })

  it('asks for no password', () => {
    const { queryByLabelText } = setup()
    expect(queryByLabelText('create.password')).toBeNull()
  })

  it('trims whitespace from username and email', async () => {
    const { getByLabelText, getByText, onCreate } = setup()
    fireEvent.change(getByLabelText('create.username'), { target: { value: '  bob  ' } })
    fireEvent.change(getByLabelText('create.email'), { target: { value: '  bob@test.com  ' } })
    fireEvent.click(getByText('create.submit'))
    await waitFor(() => expect(onCreate).toHaveBeenCalledWith('bob', 'bob@test.com', 'USER'))
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
    const onCreate = vi.fn().mockImplementation(() => new Promise(r => { resolve = () => r({ delivery: 'mail' }) }))
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
      .mockResolvedValueOnce({ delivery: 'mail' })
    const { getByLabelText, getByText, findByRole, queryByRole } = setup({ onCreate })
    fillForm(getByLabelText)
    fireEvent.click(getByText('create.submit'))
    expect((await findByRole('alert')).textContent).toContain('create.error.server')
    fireEvent.click(getByText('create.submit'))
    await waitFor(() => expect(queryByRole('alert')).toBeNull())
  })
})
