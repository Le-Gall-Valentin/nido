import { render, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { InviteMemberModal } from './InviteMemberModal'
import { NetworkError } from '@/shared/lib'
import type { SpaceInvitation } from '@/entities/space'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, opts?: Record<string, unknown>) => (opts ? `${k}:${JSON.stringify(opts)}` : k) }),
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
  Input: ({ label, name, value, onChange, disabled, placeholder, autoFocus }: {
    label: string; name: string; value: string;
    onChange: React.ChangeEventHandler<HTMLInputElement>; disabled?: boolean; placeholder?: string; autoFocus?: boolean
  }) => (
    <div>
      <label htmlFor={name}>{label}</label>
      <input id={name} name={name} value={value} onChange={onChange} disabled={disabled} placeholder={placeholder} autoFocus={autoFocus} />
    </div>
  ),
}))

const ISSUED: SpaceInvitation = {
  id: 'i-1', email: 'carol@test.com', role: 'MEMBER', code: 'NIDO-XYZ789',
  status: 'PENDING', expiresAt: '2999-01-01T00:00:00Z', createdAt: '2024-01-01T00:00:00Z',
}

const writeText = vi.fn().mockResolvedValue(undefined)
beforeEach(() => {
  vi.clearAllMocks()
  writeText.mockClear()
  Object.assign(navigator, { clipboard: { writeText } })
})

function setup(overrides: { onInvite?: () => Promise<SpaceInvitation> } = {}) {
  const onClose = vi.fn()
  const onSuccess = vi.fn()
  const onInvite = overrides.onInvite ?? vi.fn().mockResolvedValue(ISSUED)
  const result = render(<InviteMemberModal onClose={onClose} onInvite={onInvite} onSuccess={onSuccess} />)
  return { ...result, onClose, onSuccess, onInvite }
}

describe('InviteMemberModal — validation', () => {
  it('submit is disabled with an invalid email', () => {
    const { getByLabelText, getByText } = setup()
    fireEvent.change(getByLabelText('invite.email'), { target: { value: 'not-an-email' } })
    expect((getByText('invite.submit') as HTMLButtonElement).disabled).toBe(true)
  })

  it('submit is enabled with a valid email', () => {
    const { getByLabelText, getByText } = setup()
    fireEvent.change(getByLabelText('invite.email'), { target: { value: 'carol@test.com' } })
    expect((getByText('invite.submit') as HTMLButtonElement).disabled).toBe(false)
  })

  it('defaults the role to MEMBER and offers ADMIN, MEMBER, VIEWER', () => {
    const { getByRole } = setup()
    const select = getByRole('combobox') as HTMLSelectElement
    expect(select.value).toBe('MEMBER')
    expect(Array.from(select.options).map((o) => o.value)).toEqual(['ADMIN', 'MEMBER', 'VIEWER'])
  })
})

describe('InviteMemberModal — success flow', () => {
  it('shows the issued code with a copy button after a successful invite', async () => {
    const { getByLabelText, getByText, findByText } = setup()
    fireEvent.change(getByLabelText('invite.email'), { target: { value: 'carol@test.com' } })
    fireEvent.click(getByText('invite.submit'))
    expect(await findByText('NIDO-XYZ789')).toBeDefined()
  })

  it('copies the code to the clipboard', async () => {
    const { getByLabelText, getByText, findByText } = setup()
    fireEvent.change(getByLabelText('invite.email'), { target: { value: 'carol@test.com' } })
    fireEvent.click(getByText('invite.submit'))
    const codeButton = await findByText('NIDO-XYZ789')
    fireEvent.click(codeButton)
    await waitFor(() => expect(writeText).toHaveBeenCalledWith('NIDO-XYZ789'))
  })

  it('calls onSuccess when Done is clicked after issuing', async () => {
    const { getByLabelText, getByText, findByText, onSuccess } = setup()
    fireEvent.change(getByLabelText('invite.email'), { target: { value: 'carol@test.com' } })
    fireEvent.click(getByText('invite.submit'))
    await findByText('NIDO-XYZ789')
    fireEvent.click(getByText('invite.done'))
    expect(onSuccess).toHaveBeenCalledOnce()
  })
})

describe('InviteMemberModal — errors', () => {
  it('shows a network error and stays on the form', async () => {
    const { getByLabelText, getByText, findByRole, queryByText } = setup({
      onInvite: vi.fn().mockRejectedValue(new NetworkError()),
    })
    fireEvent.change(getByLabelText('invite.email'), { target: { value: 'carol@test.com' } })
    fireEvent.click(getByText('invite.submit'))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('invite.error.network')
    expect(queryByText('NIDO-XYZ789')).toBeNull()
  })

  it('calls onClose without onSuccess when cancelling the form', () => {
    const { getByText, onClose, onSuccess } = setup()
    fireEvent.click(getByText('invite.cancel'))
    expect(onClose).toHaveBeenCalledOnce()
    expect(onSuccess).not.toHaveBeenCalled()
  })
})
