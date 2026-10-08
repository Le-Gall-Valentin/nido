import { render, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { ProfileEditSection } from './ProfileEditSection'
import { ConflictError, InvalidCurrentPasswordError } from '../api/accountApi'
import { NetworkError, ServerError, RateLimitError } from '@/shared/lib'
import { ResendTooSoonError, SendLimitError } from '@/features/two-factor'
import type { User } from '@/entities/user'
import type { ProfileUpdateResult } from '../model/IAccountApi'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string, o?: Record<string, unknown>) => (o ? `${k}:${JSON.stringify(o)}` : k) }),
}))

vi.mock('./EmailCodeDialog', () => ({
  EmailCodeDialog: ({ sentTo, onConfirm, onResend, onCancel }: {
    sentTo: string; onConfirm: (code: string) => Promise<void>; onResend: () => Promise<number>; onCancel: () => void
  }) => (
    <div>
      <span>{`email-code-${sentTo}`}</span>
      <button onClick={() => void onConfirm('004213')}>confirm-code</button>
      <button onClick={() => void onResend()}>resend-code</button>
      <button onClick={onCancel}>cancel-code</button>
    </div>
  ),
}))

const BASE_USER: User = {
  id: '1', username: 'alice', email: 'alice@test.com',
  role: 'USER', createdAt: '2024-01-01T00:00:00Z', twoFactorMethods: [],
}

function setup(overrides: { onUpdateProfile?: (username: string, email: string, currentPassword?: string, emailCode?: string) => Promise<ProfileUpdateResult>; onPatch?: (partial: Partial<User>) => void } = {}) {
  const onUpdateProfile = (overrides.onUpdateProfile ?? vi.fn().mockResolvedValue({ kind: 'saved' })) as (username: string, email: string, currentPassword?: string, emailCode?: string) => Promise<ProfileUpdateResult>
  const onPatch = (overrides.onPatch ?? vi.fn()) as (partial: Partial<User>) => void
  const result = render(
    <ProfileEditSection user={BASE_USER} onPatch={onPatch} onUpdateProfile={onUpdateProfile} />
  )
  return { ...result, onUpdateProfile, onPatch }
}

describe('ProfileEditSection', () => {
  it('renders username and email fields pre-filled', () => {
    const { getByLabelText } = setup()
    expect((getByLabelText('profile.username') as HTMLInputElement).value).toBe('alice')
    expect((getByLabelText('profile.email') as HTMLInputElement).value).toBe('alice@test.com')
  })

  it('save button is disabled when fields are unchanged', () => {
    const { getByRole } = setup()
    expect((getByRole('button', { name: 'profile.save' }) as HTMLButtonElement).disabled).toBe(true)
  })

  it('save button is enabled after editing username', () => {
    const { getByLabelText, getByRole } = setup()
    fireEvent.change(getByLabelText('profile.username'), { target: { value: 'bob' } })
    expect((getByRole('button', { name: 'profile.save' }) as HTMLButtonElement).disabled).toBe(false)
  })

  it('calls onUpdateProfile with trimmed values and onPatch on success', async () => {
    const { getByLabelText, getByRole, onUpdateProfile, onPatch } = setup()
    fireEvent.change(getByLabelText('profile.username'), { target: { value: '  bob  ' } })
    fireEvent.click(getByRole('button', { name: 'profile.save' }))
    await waitFor(() => expect(onUpdateProfile).toHaveBeenCalledWith('bob', 'alice@test.com', undefined, undefined))
    expect(onPatch).toHaveBeenCalledWith({ username: 'bob', email: 'alice@test.com' })
  })

  it('shows success flash on update', async () => {
    const { getByLabelText, getByRole, findByRole } = setup()
    fireEvent.change(getByLabelText('profile.username'), { target: { value: 'bob' } })
    fireEvent.click(getByRole('button', { name: 'profile.save' }))
    const alert = await findByRole('status')
    expect(alert.textContent).toContain('profile.success')
  })

  it('shows conflict error on ConflictError', async () => {
    const { getByLabelText, getByRole, findByRole } = setup({
      onUpdateProfile: vi.fn().mockRejectedValue(new ConflictError()),
    })
    fireEvent.change(getByLabelText('profile.username'), { target: { value: 'taken' } })
    fireEvent.click(getByRole('button', { name: 'profile.save' }))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('profile.error.conflict')
  })

  it('cancel resets fields to original values', () => {
    const { getByLabelText, getByRole } = setup()
    fireEvent.change(getByLabelText('profile.username'), { target: { value: 'bob' } })
    fireEvent.click(getByRole('button', { name: 'profile.cancel' }))
    expect((getByLabelText('profile.username') as HTMLInputElement).value).toBe('alice')
  })

  it('save button is disabled and shows error when username is too short', () => {
    const { getByLabelText, getByRole, getByText } = setup()
    fireEvent.change(getByLabelText('profile.username'), { target: { value: 'ab' } })
    expect((getByRole('button', { name: 'profile.save' }) as HTMLButtonElement).disabled).toBe(true)
    expect(getByText('profile.error.username_too_short')).toBeDefined()
  })

  it('save button is disabled when the username is emptied', () => {
    const { getByLabelText, getByRole } = setup()
    fireEvent.change(getByLabelText('profile.username'), { target: { value: '   ' } })
    expect((getByRole('button', { name: 'profile.save' }) as HTMLButtonElement).disabled).toBe(true)
  })

  it('takes the username as typed — no capital or correction from a phone keyboard', () => {
    const { getByLabelText } = setup()
    const field = getByLabelText('profile.username') as HTMLInputElement
    expect(field.getAttribute('autocapitalize')).toBe('off')
    expect(field.getAttribute('autocorrect')).toBe('off')
    expect(field.getAttribute('spellcheck')).toBe('false')
  })

  it('save button is disabled and shows error when the username holds an @', () => {
    const { getByLabelText, getByRole, getByText } = setup()
    fireEvent.change(getByLabelText('profile.username'), { target: { value: 'alice@home' } })
    expect((getByRole('button', { name: 'profile.save' }) as HTMLButtonElement).disabled).toBe(true)
    expect(getByText('profile.error.username_at')).toBeDefined()
  })

  it('save button is disabled and shows error when username is too long', () => {
    const { getByLabelText, getByRole, getByText } = setup()
    fireEvent.change(getByLabelText('profile.username'), { target: { value: 'a'.repeat(51) } })
    expect((getByRole('button', { name: 'profile.save' }) as HTMLButtonElement).disabled).toBe(true)
    expect(getByText('profile.error.username_too_long')).toBeDefined()
  })

  it('shows network error on NetworkError', async () => {
    const { getByLabelText, getByRole, findByRole } = setup({
      onUpdateProfile: vi.fn().mockRejectedValue(new NetworkError()),
    })
    fireEvent.change(getByLabelText('profile.username'), { target: { value: 'bob' } })
    fireEvent.click(getByRole('button', { name: 'profile.save' }))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('profile.error.network')
  })

  it('shows server error on ServerError', async () => {
    const { getByLabelText, getByRole, findByRole } = setup({
      onUpdateProfile: vi.fn().mockRejectedValue(new ServerError()),
    })
    fireEvent.change(getByLabelText('profile.username'), { target: { value: 'bob' } })
    fireEvent.click(getByRole('button', { name: 'profile.save' }))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('profile.error.server')
  })

  it('shows rate_limit error on RateLimitError', async () => {
    const { getByLabelText, getByRole, findByRole } = setup({
      onUpdateProfile: vi.fn().mockRejectedValue(new RateLimitError()),
    })
    fireEvent.change(getByLabelText('profile.username'), { target: { value: 'bob' } })
    fireEvent.click(getByRole('button', { name: 'profile.save' }))
    const alert = await findByRole('alert')
    expect(alert.textContent).toContain('profile.error.rate_limit')
  })

  it('disables save when email is cleared', () => {
    const { getByLabelText, getByRole } = setup()
    fireEvent.change(getByLabelText('profile.email'), { target: { value: '' } })
    expect((getByRole('button', { name: 'profile.save' }) as HTMLButtonElement).disabled).toBe(true)
  })

  it('asks for the current password as soon as the address changes', () => {
    const { getByLabelText, queryByLabelText, getByRole } = setup()
    expect(queryByLabelText('profile.current_password')).toBeNull()

    fireEvent.change(getByLabelText('profile.email'), { target: { value: 'new@test.com' } })

    expect(getByLabelText('profile.current_password')).not.toBeNull()
    expect((getByRole('button', { name: 'profile.save' }) as HTMLButtonElement).disabled).toBe(true)
    fireEvent.change(getByLabelText('profile.current_password'), { target: { value: 'secret' } })
    expect((getByRole('button', { name: 'profile.save' }) as HTMLButtonElement).disabled).toBe(false)
  })

  it('sends the password with the new address', async () => {
    const { getByLabelText, getByRole, onUpdateProfile } = setup()
    fireEvent.change(getByLabelText('profile.email'), { target: { value: 'new@test.com' } })
    fireEvent.change(getByLabelText('profile.current_password'), { target: { value: 'secret' } })
    fireEvent.click(getByRole('button', { name: 'profile.save' }))

    await waitFor(() => expect(onUpdateProfile).toHaveBeenCalledWith('alice', 'new@test.com', 'secret', undefined))
  })

  it('a change of letter case only asks for no password', () => {
    const { getByLabelText, queryByLabelText, getByRole } = setup()
    fireEvent.change(getByLabelText('profile.email'), { target: { value: 'ALICE@test.com' } })

    expect(queryByLabelText('profile.current_password')).toBeNull()
    expect((getByRole('button', { name: 'profile.save' }) as HTMLButtonElement).disabled).toBe(false)
  })

  it('says so when the password is wrong, and empties it', async () => {
    const { getByLabelText, getByRole, findByRole } = setup({
      onUpdateProfile: vi.fn().mockRejectedValue(new InvalidCurrentPasswordError()),
    })
    fireEvent.change(getByLabelText('profile.email'), { target: { value: 'new@test.com' } })
    fireEvent.change(getByLabelText('profile.current_password'), { target: { value: 'wrong' } })
    fireEvent.click(getByRole('button', { name: 'profile.save' }))

    expect((await findByRole('alert')).textContent).toContain('profile.error.wrong_password')
    expect((getByLabelText('profile.current_password') as HTMLInputElement).value).toBe('')
  })

  it('forgets the typed password once the address goes back to the one it had', () => {
    const { getByLabelText } = setup()
    fireEvent.change(getByLabelText('profile.email'), { target: { value: 'new@test.com' } })
    fireEvent.change(getByLabelText('profile.current_password'), { target: { value: 'secret' } })

    fireEvent.change(getByLabelText('profile.email'), { target: { value: 'alice@test.com' } })
    fireEvent.change(getByLabelText('profile.email'), { target: { value: 'other@test.com' } })

    expect((getByLabelText('profile.current_password') as HTMLInputElement).value).toBe('')
  })

  it('ties the reason for the password to its field', () => {
    const { getByLabelText, getByText } = setup()
    fireEvent.change(getByLabelText('profile.email'), { target: { value: 'new@test.com' } })

    const hint = getByText('profile.current_password_hint')
    expect(hint.id).not.toBe('')
    expect(getByLabelText('profile.current_password').getAttribute('aria-describedby')).toBe(hint.id)
  })

  it('a new address protected by the code by mail asks for the code, then saves with it', async () => {
    const onUpdateProfile = vi.fn()
      .mockResolvedValueOnce({ kind: 'email_code_sent', sentTo: 'new@test.com', resendAfterSeconds: 60 })
      .mockResolvedValueOnce({ kind: 'saved' })
    const { getByLabelText, getByRole, findByText, getByText, onPatch } = setup({ onUpdateProfile })

    fireEvent.change(getByLabelText('profile.email'), { target: { value: 'new@test.com' } })
    fireEvent.change(getByLabelText('profile.current_password'), { target: { value: 'Secret-1' } })
    fireEvent.click(getByRole('button', { name: /profile\.save/ }))

    expect(await findByText('email-code-new@test.com')).toBeTruthy()
    expect(onPatch).not.toHaveBeenCalled()

    fireEvent.click(getByText('confirm-code'))

    await waitFor(() => expect(onUpdateProfile).toHaveBeenLastCalledWith('alice', 'new@test.com', 'Secret-1', '004213'))
    expect(onPatch).toHaveBeenCalledWith({ username: 'alice', email: 'new@test.com' })
  })

  it('asking for another code sends the same change again, without a code', async () => {
    const onUpdateProfile = vi.fn().mockResolvedValue({ kind: 'email_code_sent', sentTo: 'new@test.com', resendAfterSeconds: 60 })
    const { getByLabelText, getByRole, findByText, getByText } = setup({ onUpdateProfile })

    fireEvent.change(getByLabelText('profile.email'), { target: { value: 'new@test.com' } })
    fireEvent.change(getByLabelText('profile.current_password'), { target: { value: 'Secret-1' } })
    fireEvent.click(getByRole('button', { name: /profile\.save/ }))
    await findByText('email-code-new@test.com')
    fireEvent.click(getByText('resend-code'))

    await waitFor(() => expect(onUpdateProfile).toHaveBeenCalledTimes(2))
    expect(onUpdateProfile).toHaveBeenLastCalledWith('alice', 'new@test.com', 'Secret-1', undefined)
  })

  it('cancelling the code keeps the old address', async () => {
    const onUpdateProfile = vi.fn().mockResolvedValue({ kind: 'email_code_sent', sentTo: 'new@test.com', resendAfterSeconds: 60 })
    const { getByLabelText, getByRole, findByText, getByText, queryByText, onPatch } = setup({ onUpdateProfile })

    fireEvent.change(getByLabelText('profile.email'), { target: { value: 'new@test.com' } })
    fireEvent.change(getByLabelText('profile.current_password'), { target: { value: 'Secret-1' } })
    fireEvent.click(getByRole('button', { name: /profile\.save/ }))
    await findByText('email-code-new@test.com')
    fireEvent.click(getByText('cancel-code'))

    expect(queryByText('email-code-new@test.com')).toBeNull()
    expect(onPatch).not.toHaveBeenCalled()
  })

  it('saving again within the minute reopens the code already sent', async () => {
    // Cancelled, then saved again: the server keeps the code it sent and says so instead of sending another.
    const onUpdateProfile = vi.fn().mockRejectedValue(new ResendTooSoonError(35))
    const { getByLabelText, getByRole, findByText } = setup({ onUpdateProfile })

    fireEvent.change(getByLabelText('profile.email'), { target: { value: 'new@test.com' } })
    fireEvent.change(getByLabelText('profile.current_password'), { target: { value: 'Secret-1' } })
    fireEvent.click(getByRole('button', { name: /profile\.save/ }))

    expect(await findByText('email-code-new@test.com')).toBeTruthy()
  })

  it('too many codes sent says when another can leave', async () => {
    const onUpdateProfile = vi.fn().mockRejectedValue(new SendLimitError(600))
    const { getByLabelText, getByRole, findByText } = setup({ onUpdateProfile })

    fireEvent.change(getByLabelText('profile.email'), { target: { value: 'new@test.com' } })
    fireEvent.change(getByLabelText('profile.current_password'), { target: { value: 'Secret-1' } })
    fireEvent.click(getByRole('button', { name: /profile\.save/ }))

    expect(await findByText('profile.email_code.error.send_limit:{"minutes":10}')).toBeTruthy()
  })
})
